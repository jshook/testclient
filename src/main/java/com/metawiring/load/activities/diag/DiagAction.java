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

import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.api.ActivityAction;
import com.metawiring.load.cycler.api.ActivityDefObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DiagAction implements ActivityAction, ActivityDefObserver {

    private final static Logger logger = LoggerFactory.getLogger(DiagAction.class);

    private ActivityDef activityDef;
    private int slot;
    private long lastUpdate;
    private long quantizedInterval;


    public DiagAction(int slot, ActivityDef activityDef) {
        this.activityDef = activityDef;
        this.slot = slot;

        updateReportTime();
    }
    private void updateReportTime() {
        lastUpdate = System.currentTimeMillis() - calculateOffset(slot, activityDef);
        quantizedInterval = calculateInterval(activityDef);
        logger.debug("updating report time for slot:" + slot + ", def:" + activityDef + " to " + quantizedInterval);
    }

    @Override
    public void accept(long value) {
        long now = System.currentTimeMillis();
        if ((now - lastUpdate) > quantizedInterval) {

            logger.info("diag action, input=" + value + ", report delay=" + ((now - lastUpdate) - quantizedInterval));
            lastUpdate += quantizedInterval;
        }
    }

    private long calculateOffset(long timeslot, ActivityDef activityDef) {
        long updateInterval = activityDef.getParams().getLongOrDefault("interval", 100L);
        long offset = calculateInterval(activityDef) - (updateInterval * timeslot);
        return offset;
    }

    private long calculateInterval(ActivityDef activityDef) {
        long updateInterval = activityDef.getParams().getLongOrDefault("interval", 100L);
        int threads = activityDef.getThreads();
        return updateInterval * threads;
    }

    @Override
    public void onActivityDefUpdate(ActivityDef activityDef) {
        updateReportTime();
    }
}
