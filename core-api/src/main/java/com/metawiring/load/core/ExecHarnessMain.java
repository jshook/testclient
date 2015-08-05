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

import com.codahale.metrics.MetricRegistry;
import com.metawiring.load.activitytypes.SharedPhaseState;
import com.metawiring.load.activitytypes.SharedRunState;
import com.metawiring.load.config.PhaseDef;
import com.metawiring.load.config.TestClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class ExecHarnessMain implements Callable<SharedRunState> {

    private final static Logger logger = LoggerFactory.getLogger(ExecHarnessMain.class);
    private SharedRunState sharedRunState;

    private List<ExecHarnessPhase> execHarnessPhases = new ArrayList<>();

    public ExecHarnessMain(TestClientConfig config) {
        this.sharedRunState = new SharedRunState();
        sharedRunState.setMetrics(new MetricRegistry());
        sharedRunState.setConfig(config);
    }

    @Override
    public SharedRunState call() throws Exception {

        TestClientConfig config = sharedRunState.getConfig();

        if (config.graphiteHost != null && !config.graphiteHost.isEmpty()) {
            logger.info("Adding graphite reporter: host="
                    + config.graphiteHost + ":" + config.graphitePort
                    + " with prefix: " + config.metricsPrefix);
            sharedRunState.getMetricReporters().addGraphite(config.graphiteHost, config.graphitePort, config.metricsPrefix);
        }
        sharedRunState.getMetricReporters().start();

        ExecutorService executorService =
                Executors.newCachedThreadPool(new IndexedThreadFactory("phase-executor"));

        // Realize phases for sandbox logic
        for (PhaseDef phaseDef : sharedRunState.getConfig().getPhaseDefs()) {
            logger.info("starting phase " + phaseDef.getName());

            ExecHarnessPhase phaseHarness =
                    new ExecHarnessPhase(new SharedPhaseState(phaseDef, sharedRunState));
            execHarnessPhases.add(phaseHarness);

            logger.info("Phase executor service started, loaded " + execHarnessPhases.size() + " phases");

        }

        // Execute phases in sequence
        for (ExecHarnessPhase execHarnessPhase : execHarnessPhases) {

            Future<SharedPhaseState> phaseStateFuture = executorService.submit(execHarnessPhase);
            SharedPhaseState phaseState = null;

            while (phaseState == null) {
                try {
                    phaseState = phaseStateFuture.get();

                } catch (InterruptedException ignored) {
                } catch (ExecutionException e) {
                    logger.error("Error while launching phase harness:" + e.getMessage());
                    throw new RuntimeException(e);
                }
            }
        }

        logger.info("main executor shutting down...");
        executorService.shutdown();

        boolean isShutdown=false;
        while (!isShutdown) {
            try {
                isShutdown = executorService.awaitTermination(10000,TimeUnit.MILLISECONDS);
            }
            catch (InterruptedException ignored) {}
        }
        logger.info("main executor shutdown complete");

        return sharedRunState;
    }

}
