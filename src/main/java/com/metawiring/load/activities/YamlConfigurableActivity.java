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
import com.metawiring.load.activity.Activity;
import com.metawiring.load.activity.DefaultActivitySourceResolver;
import com.metawiring.load.config.YamlActivityDef;
import com.metawiring.load.core.ExecutionContext;
import com.metawiring.load.core.ReadyStatement;
import com.metawiring.load.core.ReadyStatements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.codahale.metrics.MetricRegistry.name;

public class YamlConfigurableActivity implements Activity {

    private static Logger logger = LoggerFactory.getLogger(YamlConfigurableActivity.class);

    private String name;
    private ExecutionContext context;
    protected boolean diagnoseExceptions = true;
    private YamlActivityDef yamlActivityDef;

    // Since this class services multiple concurrent activities, synchronize each init independently
    private static Map<String,ReadyStatements> configuredActivities = new ConcurrentHashMap<>();

    private long endCycle, submittedCycle;
    private int pendingRq = 0;

    private long maxAsync = 0l;

    private ReadyStatements readyStatements;

    private LinkedList<TimedResultSetFuture> timedResultSetFutures = new LinkedList<>();
    private Timer timerOps;
    private Timer timerWaits;
    private Counter activityAsyncPendingCounter;
    private Histogram triesHistogram;

    private static Session session;

    private String[] statements;

    private PreparedStatement[] preparedStatements;
    private static boolean staticInitComplete = false;


    @Override
    public void init(String name, ExecutionContext context) {
        this.name = name;
        this.context = context;
        this.diagnoseExceptions = context.getConfig().diagnoseExceptions;

        Yaml yaml = new Yaml();
        for (String streamname : new String[]{"activities/" + name, "activities/" + name + ".yaml"}) {
            try {
                logger.debug("Looking for " + streamname + " on filesystem.");
                InputStream stream = null;
                try {
                    stream =new FileInputStream(streamname);
                } catch (Exception ignored) {
                }
                if ( stream == null ) {
                    logger.debug("Not found on filesystem, looking for " + streamname + " on in classpath.");
                    stream = DefaultActivitySourceResolver.class.getClassLoader().getResourceAsStream(streamname);
                }
                if (stream != null) {
                    try {
                        yamlActivityDef = yaml.loadAs(stream, YamlActivityDef.class);
                    } catch (Exception ye) {
                        logger.error("error loading yaml", ye);
                        throw ye;
                    }
                }
            } catch (Exception e) {
            }
            if (yamlActivityDef != null) {
                break;
            }
        }
        if (yamlActivityDef == null) {
            throw new RuntimeException("can not configure " + YamlConfigurableActivity.class.getSimpleName() + " without a yaml stream.");
        }

    }


    @Override
    public void prepare(long startCycle, long endCycle, long maxAsync) {

        this.maxAsync = maxAsync;
        this.endCycle = endCycle;
        submittedCycle = startCycle - 1l;

        timerOps = context.getMetrics().timer(name(name, "ops-total"));
        timerWaits = context.getMetrics().timer(name(name, "ops-wait"));
        activityAsyncPendingCounter = context.getMetrics().counter(name(name, "async-pending"));
        triesHistogram = context.getMetrics().histogram(name(name, "tries-histogram"));
        // To populate the namespace
        context.getMetrics().meter(name(name, "exceptions", "PlaceHolderException"));

        if (!configuredActivities.containsKey(name)) {
            synchronized (YamlConfigurableActivity.class) {
                if (!configuredActivities.containsKey(name)) {
                    if (context.getConfig().createSchema) {
                        createSchema();
                    } else {
                        ReadyStatements rs = configureActivity(yamlActivityDef, startCycle);
                        configuredActivities.put(name,rs);
                    }
                }
            }
        }
        readyStatements = configuredActivities.get(name);

    }

    private ReadyStatements configureActivity(YamlActivityDef yamlActivityDef, long startCycle) {

        session = context.getSession();

        try {

            List<ReadyStatement> readyStatementList = new ArrayList<>();
            for (YamlActivityDef.StatementDef dmlStatement : yamlActivityDef.getDml()) {
                String statement = dmlStatement.getCookedStatement(context.getConfig());
                PreparedStatement ps = session.prepare(statement);
                ReadyStatement rs = new ReadyStatement(context, ps, startCycle);
                for (String bindname : dmlStatement.getBindNames()) {
                    String genName = dmlStatement.bindings.get(bindname);
                    if (genName==null) {
                        if (bindname.toLowerCase().equals("keyspace")) continue;
                        if (bindname.toLowerCase().equals("table")) continue;
                        throw new RuntimeException("Unable to find generator for name:" + bindname);
                    }
                    rs.addBinding(bindname, genName);
                }
                readyStatementList.add(rs);
            }

            return new ReadyStatements(readyStatementList).setConsistencyLevel(context.getConfig().defaultConsistencyLevel);
        } catch (Exception e) {
            instrumentException(e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createSchema() {

        session = context.getSession();

        for (YamlActivityDef.StatementDef statementDef: yamlActivityDef.getDdl()) {
            String qualifiedStatement = statementDef.getCookedStatement(context.getConfig());
            try {
                logger.info("Executing DDL statement:\n" + qualifiedStatement);
                session.execute(qualifiedStatement);
                logger.info("Executed DDL statement [" + qualifiedStatement + "]");
            } catch (Exception e) {
                logger.error("Error while executing statement [" + qualifiedStatement + "] " + context.getConfig().keyspace, e);
                throw new RuntimeException(e); // Let this escape, it's a critical runtime exception
            }
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
            long submittingCycle = submittedCycle + 1;
            try {

                TimedResultSetFuture trsf = new TimedResultSetFuture();
                ReadyStatement nextStatement = readyStatements.getNext(submittingCycle);

                trsf.boundStatement = nextStatement.bind();
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
            Timer.Context waitTimer = null;
            try {
                waitTimer = timerWaits.time();
                ResultSet resultSet = trsf.rsFuture.getUninterruptibly();
                waitTimer.stop();
                waitTimer = null;
                break;
            } catch (Exception e) {
                if (waitTimer != null) {
                    waitTimer.stop();
                }
                instrumentException(e);
                trsf.rsFuture = session.executeAsync(trsf.boundStatement);
                try {
                    Thread.sleep(trsf.tries * 100l);
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

    @Override
    public void cleanup() {

    }

    public YamlActivityDef getYamlActivityDef() {
        return yamlActivityDef;
    }

    private class TimedResultSetFuture {
        ResultSetFuture rsFuture;
        Timer.Context timerContext;
        BoundStatement boundStatement;
        int tries = 0;
    }

    protected void instrumentException(Exception e) {
        String exceptionType = e.getClass().getSimpleName();
        context.getMetrics().meter(name(getClass().getSimpleName(), "exceptions", exceptionType)).mark();
        if (diagnoseExceptions) {
            throw new RuntimeException(e);
        }
    }

}