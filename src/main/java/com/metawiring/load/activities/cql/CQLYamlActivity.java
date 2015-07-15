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

package com.metawiring.load.activities.cql;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.metawiring.load.activities.ActivityContextAware;
import com.metawiring.load.activity.Activity;
import com.metawiring.load.activity.TimedResultSetFuture;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.YamlActivityDef;
import com.metawiring.load.core.ExecutionContext;
import com.metawiring.load.core.ReadyStatement;
import com.metawiring.load.core.ReadyStatements;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

import static com.codahale.metrics.MetricRegistry.name;

public class CQLYamlActivity implements Activity, ActivityContextAware<CQLActivityContext> {

    private static Logger logger = LoggerFactory.getLogger(CQLYamlActivity.class);

    private long endCycle, submittedCycle;
    private int pendingRq = 0;
    private long maxAsync = 0l;

    private ReadyStatements readyStatements;
    private LinkedList<TimedResultSetFuture> timedResultSetFutures = new LinkedList<>();
    private CQLActivityContext activityContext;
    private YamlActivityDef yamlActivityDef;

    public CQLYamlActivity(YamlActivityDef yamlActivityDef) {
        this.yamlActivityDef = yamlActivityDef;
    }

    @Override
    public void prepare(long startCycle, long endCycle, long maxAsync) {

        this.maxAsync = maxAsync;
        this.endCycle = endCycle;
        submittedCycle = startCycle - 1l;

        if (activityContext.executionContext.getConfig().createSchema) {
            createSchema();
            return;
        }

        readyStatements = activityContext.getReadyStatementsTemplate().bindAllGenerators(startCycle);
    }

    @Override
    public void createSchema() {

        activityContext.createSchema();

//        for (YamlActivityDef.StatementDef statementDef : yamlActivityDef.getDdl()) {
//            String qualifiedStatement = statementDef.getCookedStatement(context.getConfig());
//            try {
//                logger.info("Executing DDL statement:\n" + qualifiedStatement);
//                session.execute(qualifiedStatement);
//                logger.info("Executed DDL statement [" + qualifiedStatement + "]");
//            } catch (Exception e) {
//                logger.error("Error while executing statement [" + qualifiedStatement + "] " + context.getConfig().keyspace, e);
//                throw new RuntimeException(e); // Let this escape, it's a critical runtime exception
//            }
//        }
//
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

                trsf.timerContext = activityContext.timerOps.time();
                trsf.rsFuture = activityContext.session.executeAsync(trsf.boundStatement);
                trsf.tries++;

                timedResultSetFutures.add(trsf);
                activityContext.activityAsyncPendingCounter.inc();
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
                waitTimer = activityContext.timerWaits.time();
                ResultSet resultSet = trsf.rsFuture.getUninterruptibly();
                waitTimer.stop();
                waitTimer = null;
                break;
            } catch (Exception e) {
                if (waitTimer != null) {
                    waitTimer.stop();
                }
                instrumentException(e);
                trsf.rsFuture = activityContext.session.executeAsync(trsf.boundStatement);
                try {
                    Thread.sleep(trsf.tries * 100l);
                } catch (InterruptedException ignored) {
                }
                trsf.tries++;
            }
        }

        pendingRq--;
        activityContext.activityAsyncPendingCounter.dec();
        trsf.timerContext.stop();
        activityContext.triesHistogram.update(trsf.tries);

    }

    @Override
    public void cleanup() {

    }

    @Override
    public CQLActivityContext createContextToShare(ActivityDef def, ScopedCachingGeneratorSource genSource, ExecutionContext executionContext) {
        CQLActivityContext activityContext = new CQLActivityContext(def, yamlActivityDef, genSource, executionContext);
        return activityContext;
    }

    @Override
    public void loadSharedContext(CQLActivityContext sharedContext) {
        this.activityContext = sharedContext;
    }

    @Override
    public Class<?> getSharedContextClass() {
        return CQLActivityContext.class;
    }

    protected void instrumentException(Exception e) {
        String exceptionType = e.getClass().getSimpleName();
        activityContext.executionContext.getMetrics().meter(name(activityContext.getActivityDef().getName(), "exceptions", exceptionType)).mark();
        if (activityContext.executionContext.getConfig().diagnoseExceptions) {
            throw new RuntimeException(e);
        }
    }

}