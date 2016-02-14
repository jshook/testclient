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
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSetFuture;
import com.google.common.collect.ImmutableMap;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.StatementDef;
import com.metawiring.load.core.MetricsContext;
import com.metawiring.load.core.OldExecutionContext;
import com.metawiring.load.core.ReadyStatements;
import com.metawiring.load.generator.GeneratorBindingList;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;


/**
 * This should be written in more of a pipeline way, with consumer and producer pools, but not now.
 */
@SuppressWarnings("ALL")
public class ReadTelemetryAsyncActivity extends BaseActivity implements ActivityContextAware<CQLActivityContext> {

    private static Logger logger = LoggerFactory.getLogger(ReadTelemetryAsyncActivity.class);

    private long endCycle, submittedCycle;
    private int pendingRq = 0;

    private long maxAsync = 0L;
    private GeneratorBindingList generatorBindingList;
    private CQLActivityContext cqlSharedContext;
    private ReadyStatements readyStatements;

    @Override
    public CQLActivityContext createContextToShare(ActivityDef def, ScopedCachingGeneratorSource genSource, OldExecutionContext executionContext) {
        CQLActivityContext activityContext = new CQLActivityContext(def, genSource, executionContext);
        List<StatementDef> statementDefs = new ArrayList<StatementDef>() {
            {

                add(
                        new StatementDef(
                                "read-recent-telemetry",

                                "     select * from <<KEYSPACE>>.<<TABLE>>_telemetry\n" +
                                        "     where source=<<source>>\n" +
                                        "     and epoch_hour=<<epoch_hour>>\n" +
                                        "     and param=<<param>>\n" +
                                        "     limit 10\n",

                                ImmutableMap.<String, String>builder()
                                        .put("source", "ThreadNumGenerator")
                                        .put("epoch_hour", "DateSequenceFieldGenerator:1000:YYYY-MM-dd-HH")
                                        .put("param", "LineExtractGenerator:data/variable_words.txt")
                                        .build()
                        )
                );
            }
        };
        activityContext.readyStatementsTemplate = activityContext.initReadyStatementsTemplate(statementDefs);
        return activityContext;
    }

    @Override
    public void loadSharedContext(CQLActivityContext cqlSharedContext) {
        this.cqlSharedContext = cqlSharedContext;
        context = cqlSharedContext.executionContext;
    }

    @Override
    public Class<?> getSharedContextClass() {
        return CQLActivityContext.class;
    }

    private class TimedResultSetFuture {
        ResultSetFuture rsFuture;
        Timer.Context timerContext;
        BoundStatement boundStatement;
        int tries = 0;
    }

    private LinkedList<TimedResultSetFuture> timedResultSetFutures = new LinkedList<>();

//    private static Session session;

    private Timer timerOps;
    private Timer timerWaits;
    private Counter activityAsyncPendingCounter;
    private Histogram triesHistogram;

    /**
     * This will be deprecated in favor of shared activity contexts in the future.
     *
     * @param startCycle
     * @param endCycle
     * @param maxAsync   - total number of async operations this activity can have pending
     */
    @Override
    public void prepare(long startCycle, long endCycle, long maxAsync) {
        this.maxAsync = maxAsync;
        this.endCycle = endCycle;
        submittedCycle = startCycle - 1l;

        timerOps = MetricsContext.metrics().timer(name(ReadTelemetryAsyncActivity.class.getSimpleName(), "ops-total"));
        timerWaits = MetricsContext.metrics().timer(name(ReadTelemetryAsyncActivity.class.getSimpleName(), "ops-wait"));

        activityAsyncPendingCounter = MetricsContext.metrics().counter(name(ReadTelemetryAsyncActivity.class.getSimpleName(), "async-pending"));

        triesHistogram = MetricsContext.metrics().histogram(name(ReadTelemetryAsyncActivity.class.getSimpleName(), "tries-histogram"));

        // To populate the namespace
        MetricsContext.metrics().meter(name(getClass().getSimpleName(), "exceptions", "PlaceHolderException"));

     readyStatements = cqlSharedContext.readyStatementsTemplate.bindAllGenerators(startCycle);


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
                trsf.boundStatement = readyStatements.getNext(submittingCycle).bind();
                trsf.timerContext = timerOps.time();
                trsf.rsFuture = cqlSharedContext.session.executeAsync(trsf.boundStatement);
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
                trsf.rsFuture.getUninterruptibly();
                waitTimer.stop();
                waitTimer = null;
                break;
            } catch (Exception e) {
                if (waitTimer != null) {
                    waitTimer.stop();
                }
                instrumentException(e);
                trsf.rsFuture = cqlSharedContext.session.executeAsync(trsf.boundStatement);
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

}