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

import com.codahale.metrics.Counter;
import com.metawiring.load.activities.ActivityContextAware;
import com.metawiring.load.activities.RuntimeContext;
import com.metawiring.load.activities.cql.ActivityContext;
import com.metawiring.load.activity.*;
import com.metawiring.load.activity.ActivityDispenserFactory;
import com.metawiring.load.activity.ActivityDispenserLocators;
import com.metawiring.load.activity.ActivityDispenser;
import com.metawiring.load.activity.ActivityHarness;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.generator.GeneratorInstantiator;
import com.metawiring.load.generator.RuntimeScope;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import com.metawiring.load.generator.ScopedGeneratorCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This class should be responsible for running a single activity to completion.
 */
public class ActivityExecutorService {

    private ExecutionContext context;
    private static Logger logger = LoggerFactory.getLogger(ActivityExecutorService.class);
    private ActivityDispenserFactory activityDispenserFactory = new ActivityDispenserLocators();

    public void prepare(ExecutionContext context) {
        this.context = context;

        Counter activityCounter = context.getMetrics().counter(name(ActivityExecutorService.class.getSimpleName(), "activities"));
        context.startup();
    }

    @SuppressWarnings("unchecked")
    public void execute() {

        ScopedCachingGeneratorSource executionScopedGeneratorCache = new ScopedGeneratorCache(new GeneratorInstantiator(),RuntimeScope.testexecution);

        List<ExecutorService> executorServices = new ArrayList<>();

        for (ActivityDef def : context.getConfig().activities) {
            logger.info("Resolving activity dispenser for " + def);

            ScopedCachingGeneratorSource activityScopedGeneratorSource = executionScopedGeneratorCache.EnterSubScope(RuntimeScope.activity);

            ActivityDispenser activityDispenser = activityDispenserFactory.get(def);

            Activity initialActivity = activityDispenser.getNewInstance();
            Object contextToShare = null;
            if (initialActivity instanceof ActivityContextAware<?>) {
                contextToShare = ((ActivityContextAware) initialActivity).createContextToShare(def,activityScopedGeneratorSource,context);
                ((ActivityContextAware) initialActivity).loadSharedContext(contextToShare); // in case we need to use this for createSchema
            }

            // TODO: This is an ugly hack too. Remove it ASAP.
            if (context.getConfig().createSchema) {
//                initialActivity.init(def.getName(), context, activityScopedGeneratorSource);
                initialActivity.prepare(0, 1, 0);
                initialActivity.createSchema();
                initialActivity.cleanup();
                continue;
            }

            ThreadFactory tf = new IndexedThreadFactory(def.toString());
            ExecutorService executorService = Executors.newFixedThreadPool(context.getConfig().createSchema ? 1 : def.getThreads(), tf);
            executorServices.add(executorService);

            logger.info("creating shared context for activity " + def.getName());
            Activity sharedContextCreator = activityDispenser.getNewInstance();

            logger.info("started thread pool " + executorService.toString());

            long threadMaxAsync = (def.getMaxAsync() / def.getThreads());
            long[] cycleRanges = getCycleRanges(def.getStartCycle(), def.getEndCycle(), def.getThreads());
            logger.info("Thread cycle ranges: " + Arrays.toString(cycleRanges));

            for (int tidx = 0; tidx < def.getThreads(); tidx++) {
                long threadStartCycle = cycleRanges[tidx * 2];
                long threadEndCycle = cycleRanges[(tidx * 2) + 1];

                ActivityHarness activityHarness = new ActivityHarness(activityDispenser, context, activityScopedGeneratorSource, threadStartCycle, threadEndCycle, threadMaxAsync, def.getInterCycleDelay(), (ActivityContext) contextToShare);
                executorService.execute(activityHarness);
                logger.info("started activity harness " + tidx + " for " + def + ", cycles: " + activityHarness.getCycleSummary());
            }

            logger.info("finished scheduling:" + def);
        }

        for (ExecutorService executorService : executorServices) {
            executorService.shutdown();
            boolean complete = false;
            while (!complete) {
                logger.info("session time: " + context.getInterval().toString());
                try {
                    complete = executorService.awaitTermination(10000, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                }
            }
            logger.info("session time: " + context.getInterval().toString());
            logger.info("Executor service has completed and shutdown.");
        }

        context.shutdown();

    }

    private long[] getCycleCounts(long startCycle, long endCycle, int threads) {
        long[] threadCycles = new long[threads];
        long totalCycles = endCycle - startCycle;
        long cycleDiv = (totalCycles / threads);
        long cycleRemainder = (totalCycles % threads);
        for (int tidx = 0; tidx < threads; tidx++) {
            threadCycles[tidx] = ((cycleDiv) + ((tidx < cycleRemainder) ? 1 : 0));
        }
        return threadCycles;
    }

    private long[] getCycleRanges(long startCycle, long endCycle, int threads) {
        long[] counts = getCycleCounts(startCycle, endCycle, threads);
        long[] ranges = new long[counts.length * 2];
        long accumulator = startCycle;
        for (int tidx = 0; tidx < counts.length; tidx++) {
            ranges[tidx * 2] = accumulator;
            ranges[(tidx * 2) + 1] = accumulator + counts[tidx];
            accumulator += (context.getConfig().splitCycles) ? counts[tidx] : 0;
        }
        return ranges;
    }

    public String toString() {
        return getClass().getSimpleName() + " context summary\n" + context.getSummary();
    }
}
