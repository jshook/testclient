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

package com.metawiring.load.activity;

import com.codahale.metrics.Counter;
import com.metawiring.load.activities.ActivityContextAware;
import com.metawiring.load.activities.oldcql.ActivityContext;
import com.metawiring.load.core.MetricsContext;
import com.metawiring.load.core.OldExecutionContext;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * An activity harness runs a single thread of an activity.
 */
public class ActivityHarness implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ActivityHarness.class);

    private final ActivityDispenser activityDispenser;
    private final OldExecutionContext context;
    private final long startCycle, endCycle, maxAsync, interCycleDelay;
    private final ScopedCachingGeneratorSource scopedGeneratorSource;
    private final ActivityContext activityContext;

    public ActivityHarness(ActivityDispenser ActivityDispenser, OldExecutionContext context, ScopedCachingGeneratorSource scopedGeneratorSource, long startCycle, long endCycle, long maxAsync, int interCycleDelay, ActivityContext activityContext) {
        this.activityDispenser = ActivityDispenser;
        this.context = context;
        this.startCycle = startCycle;
        this.endCycle = endCycle;
        this.maxAsync = maxAsync;
        this.interCycleDelay = interCycleDelay;
        this.scopedGeneratorSource = scopedGeneratorSource;
        this.activityContext = activityContext;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void run() {

        Activity activity = activityDispenser.getNewInstance();
        if (activity instanceof ActivityContextAware) {
            ((ActivityContextAware) activity).loadSharedContext(activityContext);
        }

//        activity.init(activityDispenser.getActivityName(), context, scopedGeneratorSource);
        activity.prepare(startCycle, endCycle, maxAsync);

        Counter cycleCounter = MetricsContext.metrics().counter(name(activity.getClass().getSimpleName(), "cycles"));
        cycleCounter.inc(startCycle);

        if (interCycleDelay > 0) {

            for (long cycle = startCycle; cycle < endCycle; cycle++) {
                cycleCounter.inc();
                activity.iterate();

                try {
                    Thread.sleep(interCycleDelay);
                } catch (InterruptedException ignored) {
                    // It's not worth it
                }
            }
        } else { // Hedge against the try catch performance
            for (long cycle = startCycle; cycle < endCycle; cycle++) {
                cycleCounter.inc();
                activity.iterate();
            }
        }

        activity.cleanup();

    }

    public String getCycleSummary() {
        return "(" + startCycle + ".." + endCycle + "], total=" + (endCycle - startCycle);
    }

    public String toString() {
        return getClass().getSimpleName()
                + " activitySource:" + activityDispenser
                + ", startCycle:" + startCycle
                + ", endCycle:" + endCycle
                + ", maxAsync:" + maxAsync;
    }
}
