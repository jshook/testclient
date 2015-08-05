/*
*   Copyright 2015 jshook
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
package com.metawiring.load.activitytypes;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.metawiring.load.activity.ActivityDispenser;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.TestClientConfig;
import com.metawiring.load.generator.RuntimeScope;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;

import static com.codahale.metrics.MetricRegistry.name;

public class SharedActivityState {

    private final ActivityDef activityDef;
    private final ActivityType activityType;
    private SharedPhaseState sharedPhaseState;
    private ScopedCachingGeneratorSource activityScopedCachingGeneratorSource;

    private ActivityDispenser<?> activityDispenser;

    public Timer timerOps;
    public Timer timerWaits;
    public Counter activityAsyncPendingCounter;
    public Histogram triesHistogram;

    public SharedActivityState(SharedPhaseState sharedPhaseState, ActivityType activityType, ActivityDef activityDef) {
        this.sharedPhaseState = sharedPhaseState;
        this.activityType = activityType;
        this.activityDef = activityDef;
        activityDispenser = activityType.getActivityDispenserFactory().get();

        activityScopedCachingGeneratorSource =
                sharedPhaseState
                        .getPhaseScopedCachingGeneratorSource()
                        .enterScope(RuntimeScope.activity);
        timerOps =
                sharedPhaseState.getSharedRunState().getMetrics()
                        .timer(name(activityDef.getName(), "-timer-ops"));

        timerWaits = sharedPhaseState.getSharedRunState().getMetrics()
                .timer(name(activityDef.getName(), "-timer-waits"));

        activityAsyncPendingCounter =
                sharedPhaseState.getSharedRunState().getMetrics()
                        .counter(name(activityDef.getName(), "async-pending"));
        triesHistogram =
                sharedPhaseState.getSharedRunState().getMetrics()
                        .histogram(name(activityDef.getName(), "tries-histogram"));
    }

    public ScopedCachingGeneratorSource getActivityScopedCachingGeneratorSource() {
        return activityScopedCachingGeneratorSource;
    }

    public SharedPhaseState getSharedPhaseState() {
        return sharedPhaseState;
    }

    public TestClientConfig getConfig() {
        return getSharedPhaseState().getSharedRunState().getConfig();
    }

    public ActivityType getActivityType() {
        return activityType;
    }

    public ActivityDef getActivityDef() {
        return activityDef;
    }

    public ActivityDispenser<?> getActivityDispenser() {
        return activityDispenser;
    }

    public MetricRegistry getMetrics() {
        return sharedPhaseState.getSharedRunState().getMetrics();
    }
}
