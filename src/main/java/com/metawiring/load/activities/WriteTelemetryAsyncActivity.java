/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.load.activities;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.*;
import com.metawiring.load.generator.GeneratorBindingList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

import static com.codahale.metrics.MetricRegistry.name;


/**
 * This should be written in more of a pipeline way, with consumer and producer pools, but not now.
 */
public class WriteTelemetryAsyncActivity extends BaseActivity {

    private static Logger logger = LoggerFactory.getLogger(WriteTelemetryAsyncActivity.class);

    private static final String SOURCE_FIELD = "source";
    private static final String EPOCH_HOUR_FIELD = "epoch_hour";
    private static final String PARAM_FIELD = "param";
    private static final String TIMESTAMP_FIELD = "ts";
    private static final String DATA_FIELD = "data";
    private static final String CYCLE_FIELD = "cycle";

    private static final String insertStmt = "insert into KEYSPACE.TABLE"
            + " (" + SOURCE_FIELD + "," + EPOCH_HOUR_FIELD + "," + PARAM_FIELD + "," + TIMESTAMP_FIELD + "," + DATA_FIELD + "," + CYCLE_FIELD +") values (?,?,?,?,?,?);";

//    private static final String insertStmt = "insert into " + TELEMETRY_KEYSPACE + "." + TELEMETRY_TABLE
//            + " (" + SOURCE_FIELD + "," + YEARMONDAY_FIELD + "," + PARAM_FIELD + "," + TIMESTAMP_FIELD + "," + DATA_FIELD + ") values (?,?,?,?,?);";

    private long endCycle, submittedCycle;
    private int pendingRq = 0;

    private long maxAsync = 0l;
    private GeneratorBindingList generatorBindingList;

    private class TimedResultSetFuture {
        ResultSetFuture rsFuture;
        Timer.Context timerContext;
        BoundStatement boundStatement;
        int tries = 0;
    }

    private LinkedList<TimedResultSetFuture> timedResultSetFutures = new LinkedList<>();

    private static PreparedStatement addTelemetryStmt;
    private static Session session;

    private Timer timerOps;
    private Timer timerWaits;
    private Counter activityAsyncPendingCounter;
    private Histogram triesHistogram;

    @Override
    public void prepare(long startCycle, long endCycle, long maxAsync) {
        this.maxAsync = maxAsync;
        this.endCycle = endCycle;
        submittedCycle = startCycle - 1l;

        timerOps = context.getMetrics().timer(name(WriteTelemetryAsyncActivity.class.getSimpleName(), "ops-total"));
        timerWaits = context.getMetrics().timer(name(WriteTelemetryAsyncActivity.class.getSimpleName(), "ops-wait"));

        activityAsyncPendingCounter = context.getMetrics().counter(name(WriteTelemetryAsyncActivity.class.getSimpleName(), "async-pending"));

        triesHistogram = context.getMetrics().histogram(name(WriteTelemetryAsyncActivity.class.getSimpleName(), "tries-histogram"));

        // To populate the namespace
        context.getMetrics().meter(name(getClass().getSimpleName(), "exceptions", "PlaceHolderException"));

        // This, along with static scope of the prepared stmt, will avoid "preparing the same query more than once"
        if (addTelemetryStmt == null) {
            synchronized (WriteTelemetryAsyncActivity.class) {
                if (addTelemetryStmt == null) {
                    try {
                        if (addTelemetryStmt == null) {
                            session = context.getSession();

                            if (context.getConfig().createSchema) {
                                return;
                            }

                            String statement = insertStmt;
                            statement = statement.replaceAll("KEYSPACE",context.getConfig().keyspace);
                            statement = statement.replaceAll("TABLE",context.getConfig().table);
                            addTelemetryStmt = session.prepare(statement).setConsistencyLevel(context.getConfig().defaultConsistencyLevel);
                        }
                    } catch (Exception e) {
                        instrumentException(e);
                        throw new RuntimeException(e);
                    }
                }

            }
        }

        // The names are not used as actual parameters for now, but hopefully will later
        // At least they are useful for diagnosing generator behavior
        generatorBindingList = createGeneratorBindings();
        generatorBindingList.bindGenerator(addTelemetryStmt, SOURCE_FIELD, "threadnum");
        generatorBindingList.bindGenerator(addTelemetryStmt, EPOCH_HOUR_FIELD, "date-epoch-hour", startCycle);
        generatorBindingList.bindGenerator(addTelemetryStmt, PARAM_FIELD, "varnames");
        generatorBindingList.bindGenerator(addTelemetryStmt, TIMESTAMP_FIELD, "datesecond", startCycle);
        generatorBindingList.bindGenerator(addTelemetryStmt, DATA_FIELD, "loremipsum:100:200");
        generatorBindingList.bindGenerator(addTelemetryStmt, CYCLE_FIELD, "cycle", startCycle);

    }

    @Override
    public void createSchema() {
        String keyspaceDDL = "" +
                "CREATE keyspace " + context.getConfig().keyspace +
                " with replication = {'class' : 'SimpleStrategy', 'replication_factor' : " + context.getConfig().defaultReplicationFactor + "};";

        try {
            session.execute(keyspaceDDL);
            logger.info("Created keyspace " + context.getConfig().keyspace);
        } catch (Exception e) {
            logger.error("Error while creating keyspace " + context.getConfig().keyspace, e);
            throw new RuntimeException(e); // Let this escape, it's a critical runtime exception
        }

        String tableDDL = "" +
                "CREATE table "+ context.getConfig().keyspace + "." + context.getConfig().table +
                " (\n" +
                "  source int,\n" +
                "  epoch_hour text,\n" +
                "  param text,\n" +
                "  ts timestamp,\n" +
                "  cycle bigint,\n" +
                "  data text,\n" +
                "  PRIMARY KEY ((source, epoch_hour), param, ts)\n" +
                ") WITH CLUSTERING ORDER BY (param ASC, ts DESC)";

        try {
            session.execute(tableDDL);
            logger.info("Created table " + context.getConfig().table);
        } catch (Exception e) {
            logger.error("Error while creating table " + context.getConfig().table, e);
            throw new RuntimeException(e); // Let this escape, it's a critical runtime exception
        }

    }


    /**
     * Normalize receiving rate to 1/iterate() for now, with bias on priming rq queue
     */
    @Override
    public void iterate() {

        // Not at limit, let the good times roll
        // This section fills the async pipeline to the configured limit
        while ((submittedCycle < endCycle) && (pendingRq < maxAsync)) {
            try {

                TimedResultSetFuture trsf = new TimedResultSetFuture();
                trsf.boundStatement = addTelemetryStmt.bind(generatorBindingList.getAll());
                trsf.timerContext = timerOps.time();
                trsf.rsFuture = session.executeAsync(trsf.boundStatement);
                trsf.tries++;

                timedResultSetFutures.add(trsf);
                activityAsyncPendingCounter.inc();
                pendingRq++;
                submittedCycle++;

            } catch (Exception e) {
                instrumentException(e);
            }
        }

        // This section attempts to process one async response per iteration. It always removes one from the queue,
        // and if necessary, resubmits and waits synchronously for retries. (up to 9 more times)
        int triesLimit = 10;


        TimedResultSetFuture trsf = timedResultSetFutures.pollFirst();
        if (trsf == null) {
            throw new RuntimeException("There was not a waiting future. This should never happen.");
        }

        while (trsf.tries < triesLimit) {
            Timer.Context waitTimer= null;
            try {
                waitTimer = timerWaits.time();
                trsf.rsFuture.getUninterruptibly();
                waitTimer.stop();
                waitTimer=null;
                break;
            } catch (Exception e) {
                if (waitTimer!=null) { waitTimer.stop(); }
                instrumentException(e);
                trsf.rsFuture = session.executeAsync(trsf.boundStatement);
                try {
                    Thread.sleep(trsf.tries*100l);
                } catch (InterruptedException ignored) {
                }
                trsf.tries++;
            }
        }

         pendingRq--;
         activityAsyncPendingCounter.dec();
         long duration = trsf.timerContext.stop();
         triesHistogram.update(trsf.tries);

    }

}