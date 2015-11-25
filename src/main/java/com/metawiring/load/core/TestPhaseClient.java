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


        ActivityExecutorService executorService = new ActivityExecutorService();
        logger.info("Executing main client logic");
        executorService.prepare(context);

        // TODO: Carve the metrics wiring out into something that is easier to manage and integrate. It puts too much noise here.

        // Registries come before reporters
        reporters.addRegistry("driver",context.getSession().getCluster().getMetrics().getRegistry());
        reporters.addRegistry("client",metrics);
        reporters.addLogger();
        if (config.graphiteHost != null && !config.graphiteHost.isEmpty()) {
            logger.info("Adding graphite reporter: host="
                    + config.graphiteHost + ":" + config.graphitePort
                    + " with prefix: " + config.metricsPrefix);
            reporters.addGraphite(config.graphiteHost, config.graphitePort, config.metricsPrefix);
        }
        reporters.start();

        executorService.execute();
        logger.info("Finished executing main client logic");
        Result result = new Result(context);
        context.shutdown();

        reporters.report().stop();

        return result;
    }
}
