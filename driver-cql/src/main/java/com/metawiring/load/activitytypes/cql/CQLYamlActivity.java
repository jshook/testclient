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

package com.metawiring.load.activitytypes.cql;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSet;
import com.metawiring.load.activity.Activity;
import com.metawiring.load.activitytypes.SharedActivityState;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.YamlActivityDef;
import com.metawiring.load.core.ReadyStatement;
import com.metawiring.load.core.ReadyStatements;
import com.metawiring.load.core.ReadyStatementsTemplate;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import com.metawiring.load.activitytypes.PhaseStateImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

import static com.codahale.metrics.MetricRegistry.name;

public class CQLYamlActivity implements Activity<CQLSharedActivityContext> {

    private static Logger logger = LoggerFactory.getLogger(CQLYamlActivity.class);

    private long endCycle, submittedCycle;
    private int pendingRq = 0;
    private long maxAsync = 0l;

    private ReadyStatements readyStatements;
    private LinkedList<TimedResultSetFuture> timedResultSetFutures = new LinkedList<>();
    private CQLSharedActivityContext activityState;
    private YamlActivityDef yamlActivityDef;

    public CQLYamlActivity(YamlActivityDef yamlActivityDef) {
        this.yamlActivityDef = yamlActivityDef;
    }

    @Override
    public void init(SharedActivityState sharedActivityState) {
        this.activityState = (CQLSharedActivityContext) sharedActivityState;
    }

    @Override
    public void run() {

    }

    private void iterate() {

        // Not at limit, let the good times roll
        // This section fills the async pipeline to the configured limit
        while ((submittedCycle < endCycle) && (pendingRq < maxAsync)) {
            long submittingCycle = submittedCycle + 1;
            try {

                TimedResultSetFuture trsf = new TimedResultSetFuture();
                ReadyStatement nextStatement = readyStatements.getNext(submittingCycle);

                trsf.boundStatement = nextStatement.bind();

                trsf.timerContext = activityState.timerOps.time();
                trsf.rsFuture = activityState.getSession().executeAsync(trsf.boundStatement);
                trsf.tries++;

                timedResultSetFutures.add(trsf);
                activityState.activityAsyncPendingCounter.inc();
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
                waitTimer = activityState.timerWaits.time();
                ResultSet resultSet = trsf.rsFuture.getUninterruptibly();
                waitTimer.stop();
                waitTimer = null;
                break;
            } catch (Exception e) {
                if (waitTimer != null) {
                    waitTimer.stop();
                }
                instrumentException(e);
                trsf.rsFuture = activityState.getSession().executeAsync(trsf.boundStatement);
                try {
                    Thread.sleep(trsf.tries * 100l);
                } catch (InterruptedException ignored) {
                }
                trsf.tries++;
            }
        }

        pendingRq--;
        activityState.activityAsyncPendingCounter.dec();
        trsf.timerContext.stop();

        activityState.triesHistogram.update(trsf.tries);

    }

    protected void instrumentException(Exception e) {
        String exceptionType = e.getClass().getSimpleName();
        activityState.getMetrics().meter(
                name(activityState.getActivityDef().getName(), "exceptions", exceptionType)).mark();
        if (activityState.getConfig().diagnoseExceptions) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void createSchema() {
        ReadyStatementsTemplate readyStatementsTemplate = new ReadyStatementsTemplate(
                activityState.getSession(),
                activityState.getActivityScopedCachingGeneratorSource(),
                activityState.getConfig()
        );
        readyStatementsTemplate.addStatements(yamlActivityDef, "ddl");
        readyStatementsTemplate.prepareAll();
        ReadyStatements rs = readyStatementsTemplate.bindAllGenerators(0);
        for (ReadyStatement readyStatement : rs.getReadyStatements()) {
            BoundStatement bound = readyStatement.bind();
            activityState.getSession().execute(bound);
        }

    }

}