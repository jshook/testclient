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

import com.metawiring.load.activities.cql.ActivityContext;
import com.metawiring.load.activities.cql.BaseActivityContext;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.generator.*;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * <p>An ActivityExecutor is a named instance of an execution harness for a single activity instance.
 * It is responsible for managing threads and activity settings which may be changed while the
 * activity is running.</p>
 *
 * <p>An ActivityExecutor may be represent an activity that is defined and active in the running
 * scenario, but which is inactive. This can occur when an activity is paused by controlling logic,
 * or when the threads are set to zero.</p>
 */
public class ActivityExecutor {

    private final ExecutionContext exectionContext;
    private ActivityContext activityContext;
    private ActivityDef activityDef;
    private ExecutorService executorService;

    public ActivityExecutor(ActivityDef activityDef,ExecutionContext executionContext) {
        this.activityDef = activityDef;
        this.exectionContext = executionContext;
        executorService = Executors.newCachedThreadPool(new IndexedThreadFactory(activityDef.getAlias()));
        ScopedCachingGeneratorSource gi = new ScopedGeneratorCache(new GeneratorInstantiator(), RuntimeScope.activity);
        activityContext = new BaseActivityContext(activityDef,gi,executionContext);
    }

    public void start() {
    }

    public void stop() {

    }
}
