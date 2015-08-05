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
package com.metawiring.load.core;

import com.metawiring.load.activity.*;
import com.metawiring.load.activitytypes.SharedActivityState;
import com.metawiring.load.config.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ExecHarnessPhaseActivity implements Runnable {
    private final static Logger logger = LoggerFactory.getLogger(ExecHarnessPhaseActivity.class);

    private final ActivityDef activityDef;
    private final SharedActivityState sharedActivityState;

    ExecutorService activityThreadService;

    public ExecHarnessPhaseActivity(
            SharedActivityState sharedActivityState
    ) {
        this.activityDef = sharedActivityState.getActivityDef();
        this.sharedActivityState = sharedActivityState;
    }

    @Override
    public void run() {
        activityThreadService =
                Executors.newFixedThreadPool(
                        activityDef.getThreads(),
                        new IndexedThreadFactory("activity-" + activityDef.getName())
                );

        ActivityDispenser<?> activityDispenser = sharedActivityState.getActivityType().getActivityDispenserFactory().get();

        for (int threadIndex = 0; threadIndex < activityDef.getThreads(); threadIndex++) {
            Activity<?> activity = activityDispenser.getNewInstance();
            activity.init(this.sharedActivityState);

            activityThreadService.execute(activity);
        }

        logger.info("awaiting completion and shutdown of activity:" + activityDef.getName() + "...");
        boolean shutdown=false;
        while (!shutdown) {
            try {
                shutdown= activityThreadService.awaitTermination(3600000l, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
        }
        logger.info("activity completed and shutdown:" + activityDef.getName());

    }

//    private long[] getCycleCounts(long startCycle, long endCycle, int threads) {
//        long[] threadCycles = new long[threads];
//        long totalCycles = endCycle - startCycle;
//        long cycleDiv = (totalCycles / threads);
//        long cycleRemainder = (totalCycles % threads);
//        for (int tidx = 0; tidx < threads; tidx++) {
//            threadCycles[tidx] = ((cycleDiv) + ((tidx < cycleRemainder) ? 1 : 0));
//        }
//        return threadCycles;
//    }
//
//    private long[] getCycleRanges(long startCycle, long endCycle, int threads) {
//        long[] counts = getCycleCounts(startCycle, endCycle, threads);
//        long[] ranges = new long[counts.length * 2];
//        long accumulator = startCycle;
//        for (int tidx = 0; tidx < counts.length; tidx++) {
//            ranges[tidx * 2] = accumulator;
//            ranges[(tidx * 2) + 1] = accumulator + counts[tidx];
//            accumulator += (executionContext.getConfig().splitCycles) ? counts[tidx] : 0;
//        }
//        return ranges;
//    }

//    Activity initialActivity = activityDispenser.getNewInstance();
//    Object contextToShare = null;
//    if (initialActivity instanceof ActivityContextAware<?>) {
//        contextToShare = ((ActivityContextAware) initialActivity)
//                .createContextToShare(def, activityScopedGeneratorSource, executionContext);
//        ((ActivityContextAware) initialActivity).loadSharedContext(contextToShare); // in case we need to use this for createSchema
//    }
//
//    // TODO: This is an ugly hack too. Remove it ASAP.
//    if (executionContext.getConfig().createSchema) {
////                initialActivity.init(def.getName(), context, activityScopedGeneratorSource);
//        initialActivity.prepare(0, 1, 0);
//        initialActivity.createSchema();
//        continue;
//    }
//
//    ThreadFactory tf = new IndexedThreadFactory(def.toString());
//    ExecutorService executorService =
//            Executors.newFixedThreadPool(executionContext.getConfig().createSchema ? 1 : def.getThreads(), tf);
//    logger.info("started thread pool " + executorService.toString());
//    activityExecutorServices.add(executorService);
//
//    long threadMaxAsync = (def.getMaxAsync() / def.getThreads());
//    long[] cycleRanges = getCycleRanges(def.getStartCycle(), def.getEndCycle(), def.getThreads());
//    logger.info("Thread cycle ranges: " + Arrays.toString(cycleRanges));
//
//    for (int tidx = 0; tidx < def.getThreads(); tidx++) {
//        long threadStartCycle = cycleRanges[tidx * 2];
//        long threadEndCycle = cycleRanges[(tidx * 2) + 1];
//
//        ActivityHarness activityHarness = new ActivityHarness(activityDispenser, context, activityScopedGeneratorSource, threadStartCycle, threadEndCycle, threadMaxAsync, def.getInterCycleDelay(), (SharedActivityContext) contextToShare);
//        executorService.run(activityHarness);
//        logger.info("started activity harness " + tidx + " for " + def + ", cycles: " + activityHarness.getCycleSummary());
//    }
//
//    logger.info("finished scheduling:" + def);
//}


}
