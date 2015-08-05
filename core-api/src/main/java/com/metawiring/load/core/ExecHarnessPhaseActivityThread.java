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

package com.metawiring.load.core;

import com.metawiring.load.activity.Activity;
import com.metawiring.load.activity.ActivityDispenser;
import com.metawiring.load.activitytypes.SharedActivityState;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * An activity harness runs a single thread of an activity.
 */
public class ExecHarnessPhaseActivityThread implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ExecHarnessPhaseActivityThread.class);
    private final SharedActivityState sharedActivityState;
    private Activity<?> activity;


    public ExecHarnessPhaseActivityThread(SharedActivityState sharedActivityState) {
        this.sharedActivityState = sharedActivityState;
    }

//
//    public ActivityHarness(ActivityDispenser ActivityDispenser, ExecutionContext context, ScopedCachingGeneratorSource scopedGeneratorSource, long startCycle, long endCycle, long maxAsync, int interCycleDelay, SharedActivityContext sharedActivityContext) {
//        this.activityDispenser = ActivityDispenser;
//        this.context = context;
//        this.startCycle = startCycle;
//        this.endCycle = endCycle;
//        this.maxAsync = maxAsync;
//        this.interCycleDelay = interCycleDelay;
//        this.scopedGeneratorSource = scopedGeneratorSource;
//    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {

        Activity activity = sharedActivityState.getActivityDispenser().getNewInstance();
        activity.init(sharedActivityState);



//        activity.init(activityDispenser.getActivityName(), context, scopedGeneratorSource);
//        activity.prepare(startCycle, endCycle, maxAsync);

//        Counter cycleCounter = context.getMetrics().counter(name(activity.getClass().getSimpleName(), "cycles"));
//        cycleCounter.inc(startCycle);
    }
}
