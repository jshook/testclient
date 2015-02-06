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
    private final long startCycle,endCycle,maxAsync;

    public ActivityHarness(ActivityInstanceSource ActivityInstanceSource, ExecutionContext context, long startCycle, long endCycle, long maxAsync) {
        this.ActivityInstanceSource = ActivityInstanceSource;
        this.context = context;
        this.startCycle = startCycle;
        this.endCycle = endCycle;
        this.maxAsync = maxAsync;
    }

    @Override
    public void run() {

        Activity activity = ActivityInstanceSource.get();

        activity.init(ActivityInstanceSource.getActivityName(), context);
        activity.prepare(startCycle,endCycle,maxAsync);

        Counter cycleCounter = context.getMetrics().counter(name(activity.getClass().getSimpleName(),"cycles"));
        cycleCounter.inc(startCycle);
        for (long cycle = startCycle; cycle < endCycle; cycle++) {
            cycleCounter.inc();
            activity.iterate();
        }

        activity.cleanup();

    }

    public String getCycleSummary() {
        return "(" + startCycle + ".." + endCycle + "], total=" + (endCycle-startCycle);
    }

    public String toString() {
        return getClass().getSimpleName()
                + " activitySource:" + ActivityInstanceSource
                + ", startCycle:" + startCycle
                + ", endCycle:" + endCycle
                + ", maxAsync:" + maxAsync;
    }
}
