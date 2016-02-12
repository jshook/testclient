/*
*   Copyright 2016 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
package com.metawiring.load.activities.cql;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.ResultSet;
import com.metawiring.load.activity.TimedResultSetFuture;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.core.MetricsContext;
import com.metawiring.load.core.ReadyStatement;
import com.metawiring.load.core.ReadyStatements;
import com.metawiring.load.cycler.api.ActivityAction;
import com.metawiring.load.cycler.api.ActivityDefObserver;

import static com.codahale.metrics.MetricRegistry.name;

public class CQLAction implements ActivityAction, ActivityDefObserver {

    private final int slot;
    private ActivityDef activityDef;
    private ReadyStatements readyStatements;
    CQLActionContext activityContext;

    // optional parameters
    int triesLimit = 10;
    boolean throwDiagnosticExceptionss=true;
    long retryDelay = 100L;

    public CQLAction(int slot, ActivityDef activityDef) {
        this.activityDef = activityDef;
        this.slot = slot;
        onActivityDefUpdate(activityDef);
    }

    @Override
    public void accept(long value) {


        // Top section is enqueue. This code emulates the two-phase structure of an
        // async prime + drain workload, although it is strictly thread-per-op.
        // This is to show the affinity between the two approaches with respect to the async API

        TimedResultSetFuture trsf = new TimedResultSetFuture();
        ReadyStatement nextStatement = readyStatements.getNext(value);
        trsf.boundStatement = nextStatement.bind();

        trsf.timerContext = activityContext.timerOps.time();
        trsf.rsFuture = this.activityContext.getSession().executeAsync(trsf.boundStatement);
        trsf.tries++;
        activityContext.activityAsyncPendingCounter.inc();


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
                trsf.rsFuture = activityContext.getSession().executeAsync(trsf.boundStatement);
                try {
                    Thread.sleep(trsf.tries * retryDelay);
                } catch (InterruptedException ignored) {
                }
                trsf.tries++;
            }
        }

    }

    protected void instrumentException(Exception e) {
        String exceptionType = e.getClass().getSimpleName();

        MetricsContext.metrics().meter(
                name(activityContext.getActivityDef().getAlias(), "exceptions", exceptionType)
        ).mark();

        if (throwDiagnosticExceptionss) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        triesLimit = activityDef.getParams().getIntOrDefault("tries", 10);
        throwDiagnosticExceptionss = activityDef.getParams().getBoolOrDefault("diag",false);
        retryDelay = activityDef.getParams().getLongOrDefault("retrydelay",100L);

    }
}
