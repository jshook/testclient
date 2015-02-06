package com.metawiring.load.activities;

import com.metawiring.load.activity.Activity;
import com.metawiring.load.core.ExecutionContext;
import com.metawiring.load.core.IndexedThreadFactory;
import com.metawiring.load.generator.GeneratorBindings;

import static com.codahale.metrics.MetricRegistry.name;

public abstract class BaseActivity implements Activity {
    protected ExecutionContext context;
    protected String activityName;
    protected boolean diagnoseExceptions = true;

    @Override
    public final void init(String activityName, ExecutionContext context) {
        this.activityName=activityName;
        this.context = context;
        this.diagnoseExceptions = context.getConfig().diagnoseExceptions;
    }

    @Override
    public void cleanup() { }

    protected String getThreadMetricName(String metricIdentifier) {
        String threadName = ((IndexedThreadFactory.IndexedThread) Thread.currentThread()).getMetricName();
        int threadIndex = ((IndexedThreadFactory.IndexedThread) Thread.currentThread()).getThreadIndex();
        String threadMetricName = name(
                getClass().getSimpleName(),
                threadName,
                String.valueOf(threadIndex),
                metricIdentifier);
        return threadMetricName;
    }
    protected void setThreadMetricBasename(String threadMetricName) {
        ((IndexedThreadFactory.IndexedThread) Thread.currentThread()).setMetricName(threadMetricName);
    }

    protected void instrumentException(Exception e) {
        String exceptionType = e.getClass().getSimpleName();
        context.getMetrics().meter(name(getClass().getSimpleName(), "exceptions", exceptionType)).mark();
        if (diagnoseExceptions) {
            throw new RuntimeException(e);
        }
    }

    protected GeneratorBindings createGeneratorBindings() {
        return new GeneratorBindings(context.getGeneratorInstanceSource());
    }

}

