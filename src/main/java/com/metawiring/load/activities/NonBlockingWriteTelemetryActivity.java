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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.metawiring.load.activity.TimedResultSetFuture;
import com.metawiring.load.generator.GeneratorBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;


/**
 * This should be written in more of a pipeline way, with consumer and producer pools, but not now.
 */
public class NonBlockingWriteTelemetryActivity extends BaseActivity {

    private static Logger logger = LoggerFactory.getLogger(NonBlockingWriteTelemetryActivity.class);

    private static final String SOURCE_FIELD = "source";
    private static final String EPOCH_HOUR_FIELD = "epoch_hour";
    private static final String PARAM_FIELD = "param";
    private static final String TIMESTAMP_FIELD = "ts";
    private static final String DATA_FIELD = "data";
    private static final String CYCLE_FIELD = "cycle";

    private static final String insertStmt = "insert into KEYSPACE.TABLE"
            + " (" + SOURCE_FIELD + "," + EPOCH_HOUR_FIELD + "," + PARAM_FIELD + "," + TIMESTAMP_FIELD + "," + DATA_FIELD + "," + CYCLE_FIELD + ") values (?,?,?,?,?,?);";

//    private static final String insertStmt = "insert into " + TELEMETRY_KEYSPACE + "." + TELEMETRY_TABLE
//            + " (" + SOURCE_FIELD + "," + YEARMONDAY_FIELD + "," + PARAM_FIELD + "," + TIMESTAMP_FIELD + "," + DATA_FIELD + ") values (?,?,?,?,?);";

    private long endCycle, submittedCycle;
    private AtomicInteger pendingRq = new AtomicInteger();


    private long maxAsync = 0l;

    private GeneratorBindings generatorBindings;

    private Histogram opsHistogram;

    //private List<TimedResultSetFuture> timedResultSetFutures = null;

    private static PreparedStatement addTelemetryStmt;
    private static Session session;

    private Timer writeTimerOps;
    private Timer writerTimerWaits;
    private Counter activityAsyncPendingCounter;
    private Histogram triesHistogram;

    int triesLimit = 10;

    @Override
    public void prepare(long startCycle, long endCycle, long maxAsync) {
        this.maxAsync = maxAsync;
        this.endCycle = endCycle;
        submittedCycle = startCycle - 1l;

        writeTimerOps = context.getMetrics().timer(name(NonBlockingWriteTelemetryActivity.class.getSimpleName(), "ops-total"));
        writerTimerWaits = context.getMetrics().timer(name(NonBlockingWriteTelemetryActivity.class.getSimpleName(), "ops-wait"));

        activityAsyncPendingCounter = context.getMetrics().counter(name(NonBlockingWriteTelemetryActivity.class.getSimpleName(), "async-pending"));

        triesHistogram = context.getMetrics().histogram(name(NonBlockingWriteTelemetryActivity.class.getSimpleName(), "tries-histogram"));
        opsHistogram = context.getMetrics().histogram(name(NonBlockingWriteTelemetryActivity.class.getSimpleName(), "ops-histogram"));

        // To populate the namespace
        context.getMetrics().meter(name(getClass().getSimpleName(), "exceptions", "PlaceHolderException"));

        // This, along with static scope of the prepared stmt, will avoid "preparing the same query more than once"
        if (addTelemetryStmt == null) {
            synchronized (NonBlockingWriteTelemetryActivity.class) {
                if (addTelemetryStmt == null) {
                    try {
                        if (addTelemetryStmt == null) {
                            session = context.getSession();

                            if (context.getConfig().createSchema) {
                                return;
                            }

                            String statement = insertStmt;
                            statement = statement.replaceAll("KEYSPACE", context.getConfig().getKeyspace());
                            statement = statement.replaceAll("TABLE", context.getConfig().getTable());
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
        generatorBindings = createGeneratorBindings();
        generatorBindings.bindGenerator(addTelemetryStmt, SOURCE_FIELD, "threadnum");
        generatorBindings.bindGenerator(addTelemetryStmt, EPOCH_HOUR_FIELD, "date-epoch-hour", startCycle);
        generatorBindings.bindGenerator(addTelemetryStmt, PARAM_FIELD, "varnames");
        generatorBindings.bindGenerator(addTelemetryStmt, TIMESTAMP_FIELD, "datesecond", startCycle);
        generatorBindings.bindGenerator(addTelemetryStmt, DATA_FIELD, "loremipsum:100:200");
        generatorBindings.bindGenerator(addTelemetryStmt, CYCLE_FIELD, "cycle", startCycle);
    }

    @Override
    public void createSchema() {
        String keyspaceDDL = "" +
                "CREATE keyspace " + context.getConfig().getKeyspace() +
                " with replication = {'class' : 'SimpleStrategy', 'replication_factor' : " + context.getConfig().defaultReplicationFactor + "};";

        try {
            session.execute(keyspaceDDL);
            logger.info("Created keyspace " + context.getConfig().getKeyspace());
        } catch (Exception e) {
            logger.error("Error while creating keyspace " + context.getConfig().getKeyspace(), e);
            throw new RuntimeException(e); // Let this escape, it's a critical runtime exception
        }

        String tableDDL = "" +
                "CREATE table " + context.getConfig().getKeyspace() + "." + context.getConfig().getTable() +
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
            logger.info("Created table " + context.getConfig().getTable());
        } catch (Exception e) {
            logger.error("Error while creating table " + context.getConfig().getTable(), e);
            throw new RuntimeException(e); // Let this escape, it's a critical runtime exception
        }

    }


    /**
     * Normalize receiving rate to 1/iterate() for now, with bias on priming rq queue
     */
    @Override
    public void iterate() {

        List<TimedResultSetFuture> timedResultSetFuturesList = Lists.newArrayListWithExpectedSize((int) maxAsync);

        // Not at limit, let the good times roll
        // This section fills the async pipeline to the configured limit
        while ((submittedCycle < endCycle) && (pendingRq.get() < maxAsync)) {
            try {

                TimedResultSetFuture trsf = new TimedResultSetFuture();
                trsf.boundStatement = addTelemetryStmt.bind(generatorBindings.getAll());
                trsf.timerContext = writeTimerOps.time();
                trsf.rsFuture = session.executeAsync(trsf.boundStatement);
                trsf.tries++;

                timedResultSetFuturesList.add(trsf);

                activityAsyncPendingCounter.inc();
                pendingRq.incrementAndGet();
                submittedCycle++;


            } catch (Exception e) {
                instrumentException(e);
            }
        }

        ImmutableList<ListenableFuture<TimedResultSetFuture>> listenableFutures = Futures.inCompletionOrder(timedResultSetFuturesList);

        for (ListenableFuture<TimedResultSetFuture> future : listenableFutures) {
            try {
                Timer.Context waitTimer = null;

                waitTimer = writerTimerWaits.time();
                TimedResultSetFuture trsf = future.get();

                if (waitTimer != null) {
                    waitTimer.stop();
                }


                activityAsyncPendingCounter.dec();
                pendingRq.decrementAndGet();

                long duration = trsf.timerContext.stop();

                // This wasn't in the original WriteTelemetryAsyncActivity - is it needed?
                // opsHistogram.update(duration);
                triesHistogram.update(trsf.tries);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        }
    }



}
