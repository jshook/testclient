/*
*   Copyright 2016 jshook
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
package com.metawiring.load.activities.diag;

import com.google.auto.service.AutoService;
import com.metawiring.load.activities.ActivityType;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.*;
import com.metawiring.load.cycler.api.*;
import com.metawiring.load.cycler.inputs.CycleSequenceSupplier;
import com.metawiring.load.cycler.motors.ActivityMotor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The DiagActivty, aka "diag", is simply a diagnostic activity.
 * It logs the input to priority INFO on some interval, in milliseconds.
 * Each interval, one of the activities will report both the current input value and
 * the number of milliseconds that have elapsed since the activity was scheduled to report.
 *
 * It is built-in to the core TestClient codebase, and always available for sanity checks.
 * It is also the default activity that is selected if no activity type is specified nor inferred.
 * It serves as a basic template for implementing your own activity type.
 */
@AutoService(ActivityType.class)
public class DiagActivity implements ActivityType, ActivityInputDispenser, MotorDispenser {

    // TODO: Allow Activity Types to optionally implement ActivityMotorFactory
    // TODO: for activities that need to take control of the activity at a deeper level.

    private static final Logger logger = LoggerFactory.getLogger(DiagActivity.class);

    @Override
    public String getName() {
        return "diag";
    }

    @Override
    public <T extends ActivityDef> ActionDispenser getActionDispenser(T activityDef) {
        return DiagAction::new;
    }

    @Override
    public ActivityInput getInput(ActivityDef activityDef) {
        ActivityInput input = new CycleSequenceSupplier().setRange(activityDef.getStartCycle(), activityDef.getEndCycle());
        return input;
    }

    private MotorDispenser motorDispenser;
    /**
     * This motor dispenser method is redundant, but illustrative. It shows how to create the same
     * type of motor that would be used even if DiagActivity didn't implement MotorDispenser.
     * @param activityDef - The activity definition to be supported by the motor.
     * @param motorId - The slot nuber of the motor.
     * @return
     */
    @Override
    public ActivityMotor getMotor(ActivityDef activityDef, int motorId) {
        if (motorDispenser == null) {
            synchronized (this) {
                if (motorDispenser == null) {
                    motorDispenser = new CoreMotorDispenser(getInput(activityDef), getActionDispenser(activityDef));
                }
            }
        }
        return motorDispenser.getMotor(activityDef, motorId);
    }

}
