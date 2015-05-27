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
import com.metawiring.load.core.ExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * An activity harness runs a single thread of an activity.
 */
public class ActivityHarness implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(ActivityHarness.class);

    private final ActivityInstanceSource ActivityInstanceSource;
    private final ExecutionContext context;
    private final long startCycle, endCycle, maxAsync, interCycleDelay;

    public ActivityHarness(ActivityInstanceSource ActivityInstanceSource, ExecutionContext context, long startCycle, long endCycle, long maxAsync, int interCycleDelay) {
        this.ActivityInstanceSource = ActivityInstanceSource;
        this.context = context;
        this.startCycle = startCycle;
        this.endCycle = endCycle;
        this.maxAsync = maxAsync;
        this.interCycleDelay = interCycleDelay;
    }

    @Override
    public void run() {

        Activity activity = ActivityInstanceSource.get();

        activity.init(ActivityInstanceSource.getActivityName(), context);
        activity.prepare(startCycle, endCycle, maxAsync);

        Counter cycleCounter = context.getMetrics().counter(name(activity.getClass().getSimpleName(), "cycles"));
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
        } else { // Hedge against the try catch
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
                + " activitySource:" + ActivityInstanceSource
                + ", startCycle:" + startCycle
                + ", endCycle:" + endCycle
                + ", maxAsync:" + maxAsync;
    }
}
