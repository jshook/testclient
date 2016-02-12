package com.metawiring.load.cycler;

import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.api.ActivityAction;
import com.metawiring.load.cycler.api.ActivityInput;
import com.metawiring.load.cycler.api.MotorDispenser;
import com.metawiring.load.cycler.inputs.CycleSequenceSupplier;
import com.metawiring.load.cycler.motors.ActivityMotor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import java.util.function.LongConsumer;

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
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either exNpress or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
@Test(enabled=false)
public class ActivityExecutorTest {
    private static final Logger logger = LoggerFactory.getLogger(ActivityExecutorTest.class);

    @Test(enabled=false)
    public void testNewActivityExecutor() {
        ActivityDef ad = ActivityDef.parseActivityDef("alias=test");
        ActivityExecutor ae = new ActivityExecutor(ad);
        ActivityInput longSupplier = new CycleSequenceSupplier();
        MotorDispenser cmf = getCycleMotorFactory(
                ad, motorActionDelay(999), longSupplier
        );
        ae.setActivityMotorDispenser(cmf);
        ad.setThreads(5);
        ae.start();

        int[] speeds = new int[]{1,2000,5,2000,2,2000};
        for(int offset=0; offset<speeds.length; offset+=2) {
            int threadTarget=speeds[offset];
            int threadTime = speeds[offset+1];
            logger.info("Setting thread level to " + threadTarget + " for " +threadTime + " seconds.");
            ad.setThreads(threadTarget);
            try {
                Thread.sleep(threadTime);
            } catch (InterruptedException ignored) {
            }
        }
        ad.setThreads(0);

    }

    private MotorDispenser getCycleMotorFactory(final ActivityDef ad, ActivityAction lc, final ActivityInput ls) {
        MotorDispenser cmf = new MotorDispenser() {
            @Override
            public ActivityMotor getMotor(ActivityDef activityDef, int motorId) {
                ActivityMotor cm = new ActivityMotor(motorId, ls);
                cm.setAction(lc);
                return cm;
            }
        };
        return cmf;
    }

    private ActivityAction motorActionDelay(final long delay) {
        ActivityAction consumer = new ActivityAction() {
            @Override
            public void accept(long value) {
                System.out.println("consuming " + value + ", delaying:" + delay);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ignored) {
                }
            }
        };
        return consumer;
    }
}