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

import com.metawiring.load.activity.ActivityDispenser;
import com.metawiring.load.activity.ActivityDispenserFactory;
import com.metawiring.load.activity.ActivityTypeLocator;
import com.metawiring.load.activitytypes.*;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.PhaseDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class ExecHarnessPhase implements Callable<SharedPhaseState> {
    private final static Logger logger = LoggerFactory.getLogger(ExecHarnessPhase.class);
    private final PhaseDef phaseDef;
    private final ActivityTypeLocator activityTypeLocator;

    ExecutorService activitiesExecutor;
    List<ExecHarnessPhaseActivity> subHarnessList = new ArrayList<>();
    SharedPhaseState sharedPhaseState;

    public ExecHarnessPhase(SharedPhaseState sharedPhaseState) {
        this.sharedPhaseState = sharedPhaseState;
        this.activityTypeLocator = sharedPhaseState.getSharedRunState().getActivityTypeLocator();
        this.phaseDef = sharedPhaseState.getPhaseDef();
    }

    @Override
    public SharedPhaseState call() throws Exception {

        activitiesExecutor =
                Executors.newFixedThreadPool(
                        phaseDef.getActivityDefs().size(),
                        new IndexedThreadFactory("phase-" + phaseDef.getName())
                );

        for (ActivityDef activityDef : phaseDef.getActivityDefs()) {
            logger.info("Resolving activity dispenser for " + activityDef);

            Optional<ActivityType> activityTypeLookup = activityTypeLocator.resolveActivityType(activityDef);
            ActivityType activityType = activityTypeLookup.orElseThrow(new Supplier<RuntimeException>() {
                @Override
                public RuntimeException get() {
                    return new RuntimeException("Unable to find an activity type for activityDef:" + activityDef);
                }
            });

            SharedActivityState sharedActivityState = activityType.getSharedActivityState(sharedPhaseState, activityType, activityDef);
            ExecHarnessPhaseActivity activityHarness = new ExecHarnessPhaseActivity(sharedActivityState);

            subHarnessList.add(activityHarness);
            activitiesExecutor.execute(activityHarness);
        }

        activitiesExecutor.shutdown();
        boolean complete = false;
        while (!complete) {
            logger.info("awaiting shutdown of phase executor harness '" + phaseDef.getName() + "'...");
            try {
                complete = activitiesExecutor.awaitTermination(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
            logger.info("phase executor shutdown complete");
        }

        return sharedPhaseState;
    }

}
