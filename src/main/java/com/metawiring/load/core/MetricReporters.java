package com.metawiring.load.core;

import com.codahale.metrics.*;
import com.codahale.metrics.graphite.Graphite;
import com.codahale.metrics.graphite.GraphiteReporter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MetricReporters {
    private final static Logger logger = LoggerFactory.getLogger(MetricReporters.class);
    private static MetricReporters instance = new MetricReporters();

    private List<ScheduledReporter> scheduledReporters = new ArrayList<>();

    private MetricReporters() {
    }

    public static MetricReporters getInstance() {
        return instance;
    }

    public MetricReporters addGraphite(MetricRegistry registry, String host, int graphitePort, String prefix) {

        Graphite graphite = new Graphite(new InetSocketAddress(host, graphitePort));
        GraphiteReporter graphiteReporter = GraphiteReporter.forRegistry(registry)
                .prefixedWith(prefix)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .build(graphite);

        scheduledReporters.add(graphiteReporter);
        return this;
    }

//    public MetricReporters addConsole(MetricRegistry registry) {
//        ConsoleReporter consoleReporter = ConsoleReporter.forRegistry(registry)
//                .convertRatesTo(TimeUnit.SECONDS)
//                .convertDurationsTo(TimeUnit.MILLISECONDS)
//                .filter(MetricFilter.ALL)
//                .build();
//
//        scheduledReporters.add(consoleReporter);
//        return this;
//    }

    public MetricReporters addLogger(MetricRegistry registry) {
        Slf4jReporter loggerReporter = Slf4jReporter.forRegistry(registry)
                .convertRatesTo(TimeUnit.SECONDS)
                .convertDurationsTo(TimeUnit.MILLISECONDS)
                .filter(MetricFilter.ALL)
                .outputTo(logger)
                .build();
        scheduledReporters.add(loggerReporter);
        return this;
    }

    public MetricReporters start() {
        for (ScheduledReporter scheduledReporter : scheduledReporters) {
            logger.info("starting reporter: " + scheduledReporter);
            if (scheduledReporter instanceof ConsoleReporter) {
                scheduledReporter.start(10, TimeUnit.SECONDS);
            } else {
                scheduledReporter.start(1, TimeUnit.MINUTES);
            }
        }
        return this;
    }

    public MetricReporters stop() {
        for (ScheduledReporter scheduledReporter : scheduledReporters) {
            logger.info("stopping reporter: " + scheduledReporter);
            scheduledReporter.stop();
        }
        return this;
    }


    public MetricReporters report() {
        for (ScheduledReporter scheduledReporter : scheduledReporters) {
            logger.info("flushing reporter data: " + scheduledReporter);
            scheduledReporter.report();
        }
        return this;
    }
}
