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
package com.metawiring.load.cycler;

import com.metawiring.load.cycler.api.ActionDispenser;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.api.ActivityAction;
import com.metawiring.load.cycler.api.ActivityInput;
import com.metawiring.load.cycler.api.MotorDispenser;
import com.metawiring.load.cycler.motors.ActivityMotor;
import com.metawiring.load.cycler.motors.CoreActivityMotor;

/**
 * Produce index ActivityMotor instances with an input and action,
 * given the input and an action factory.
 */
public class CoreMotorDispenser implements MotorDispenser {

    private ActivityInput input;
    private ActionDispenser actionDispenser;

    public CoreMotorDispenser(ActivityInput input, ActionDispenser actionDispenser) {
        this.input = input;
        this.actionDispenser = actionDispenser;
    }

    @Override
    public ActivityMotor getMotor(ActivityDef activityDef, int motorId) {
        ActivityAction action = actionDispenser.getAction(motorId,activityDef);
        ActivityMotor am = new CoreActivityMotor(motorId,input,action);
        return am;
    }
}
