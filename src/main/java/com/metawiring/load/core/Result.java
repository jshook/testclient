package com.metawiring.load.core;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.MetricFilter;

import java.io.PrintStream;
import java.util.concurrent.TimeUnit;

public class Result {
    private ExecutionContext context;

    public Result(ExecutionContext context) {
        this.context = context;
    }

    public void reportTo(PrintStream out) {
        ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(context.getMetrics())
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .convertRatesTo(TimeUnit.SECONDS)
                .filter(MetricFilter.ALL)
                .outputTo(out)
                .build();
        consoleReporter.report();
    }
}
