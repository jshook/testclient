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
package com.metawiring.load.cycler.motors;

import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.MotorController;
import com.metawiring.load.cycler.api.ActivityAction;
import com.metawiring.load.cycler.api.ActivityInput;
import com.metawiring.load.cycler.api.ActivityDefObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>CycleMotor is a Runnable which runs in one of an activity's many threads.
 * It is the iteration harness for individual cycles of an Activity. Each CycleMotor
 * instance is responsible for taking input from a LongSupplier and applying
 * the provided LongConsumer to it on each cycle. These two parameters are called
 * input and action, respectively.
 * <p/>
 */
public class ActivityMotor implements Runnable, ActivityDefObserver {
    private static final Logger logger = LoggerFactory.getLogger(ActivityMotor.class);

    private long motorId;
    private final MotorController motorController = new MotorController(this);
    private ActivityInput input;
    private ActivityAction action;

    /**
     * Create a CycleMotor.
     *
     * @param motorId The enumeration of the motor, as assigned by its executor.
     * @param input   A LongSupplier which provides the cycle number inputs.
     */
    public ActivityMotor(
            long motorId,
            ActivityInput input) {
        this.motorId = motorId;
        setInput(input);
    }

    /**
     * Create a CycleMotor.
     *
     * @param motorId The enumeration of the motor, as assigned by its executor.
     * @param input   A LongSupplier which provides the cycle number inputs.
     * @param action  An LongConsumer which is applied to the input for each cycle.
     */
    public ActivityMotor(
            long motorId,
            ActivityInput input,
            ActivityAction action
    ) {
        this(motorId, input);
        setAction(action);
    }

    /**
     * Set the input for this CycleMotor.
     *
     * @param input The LongSupplier that provides the cycle number.
     * @return this CycleMotor, for chaining
     */
    public ActivityMotor setInput(ActivityInput input) {
        this.input = input;
        return this;
    }

    /**
     * Set the action for this CycleMotor.
     *
     * @param action The LongConsumer that will be applied to the next cycle number.
     * @return this CycleMotor, for chaining
     */
    public ActivityMotor setAction(ActivityAction action) {
        this.action = action;
        return this;
    }

    public MotorController getMotorController() {
        return motorController;
    }

    @Override
    public void run() {
        motorController.signalStarted();
        long cyclenum;

        while (motorController.getRunState() == MotorController.RunState.Started) {
            cyclenum = input.getAsLong();
            // TODO: memoize max for some stride, or find out how to reduce signaling overhead here
            // TODO: Figure out how to signal any control logic to avoid or react to graceful motor exits with spurious attempts to restart
            if (cyclenum > input.getMax()) {
                logger.trace("input exhausted (input " + cyclenum + "), stopping motor thread " + motorId);
                motorController.requestStop();
                continue;
            }
            logger.trace("cycle " + cyclenum);
            action.accept(cyclenum);
        }

        // TODO:zero-pad activity motor identifiers in log outputs
        motorController.signalStopped();
    }

    public boolean hasStarted() {
        return (motorController.getRunState() == MotorController.RunState.Started);
    }

    @Override
    public String toString() {
        return "motor:" + this.motorId + "; state:" + this.getMotorController();
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        if (input instanceof ActivityDefObserver) {
            ((ActivityDefObserver) input).onActivityDefUpdate(activityDef);
        }
        if (action instanceof ActivityDefObserver) {
            ((ActivityDefObserver) action).onActivityDefUpdate(activityDef);
        }
    }
}
