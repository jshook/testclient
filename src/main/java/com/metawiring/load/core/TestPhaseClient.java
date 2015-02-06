package com.metawiring.load.core;

import com.codahale.metrics.MetricRegistry;
import com.metawiring.load.cli.TestClientCLIOptions;
import com.metawiring.load.config.TestClientConfig;
import org.slf4j.Logger;

import java.util.concurrent.Callable;

public class TestPhaseClient implements Callable<Result> {

    ExecutionContext context;
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(TestPhaseClient.class);

    public TestPhaseClient configure(String[] args) {
        TestClientConfig config = new TestClientCLIOptions().parse(args);
        context = new ExecutionContext(config);
        return this;
    }

    @Override
    public Result call() throws Exception {

        MetricRegistry metrics = context.getMetrics();
        MetricReporters reporters = MetricReporters.getInstance();
        TestClientConfig config = context.getConfig();
        reporters.addLogger(metrics);

        if (config.graphiteHost != null && !config.graphiteHost.isEmpty()) {
            logger.info("Adding graphite reporter: host="
                    + config.graphiteHost + ":" + config.graphitePort
                    + " with prefix: " + config.metricsPrefix);
            reporters.addGraphite(metrics, config.graphiteHost, config.graphitePort, config.metricsPrefix);
        }
        reporters.start();

        ActivityExecutorService executor = new ActivityExecutorService();
        logger.info("Executing main client logic");
        executor.execute(context);
        logger.info("Finished executing main client logic");
        Result result = new Result(context);
        context.shutdown();

        reporters.report().stop();

        return result;
    }
}
