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

import com.metawiring.load.config.ActivityDef;

import java.util.HashMap;
import java.util.Map;

public class StageController {

    private ExecutionContext context;
    private final Map<String, ActivityExecutor> activityExecutors = new HashMap<>();

    public void prepare(ExecutionContext executionContext) {
        this.context = executionContext;
    }

    public synchronized void start(ActivityDef activityDef) {
        getActivityExecutor(activityDef).start();
    }

    public synchronized void stop(ActivityDef activityDef) {
        getActivityExecutor(activityDef).stop();
    }

    private ActivityExecutor getActivityExecutor(ActivityDef activityDef) {
        synchronized (activityExecutors) {
            ActivityExecutor ae = activityExecutors.get(activityDef.getAlias());
            if (ae == null) {
                ae = new ActivityExecutor(activityDef, context);
                activityExecutors.put(activityDef.getAlias(), ae);
            }
            return ae;
        }
    }
}
