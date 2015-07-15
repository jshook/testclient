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

package com.metawiring.load.activities.cql;

import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.core.ExecutionContext;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;

public class BaseActivityContext implements ActivityContext {
    private final ScopedCachingGeneratorSource scopedCachingGeneratorSource;
    private final ActivityDef activityDef;
    private final ExecutionContext executionContext;

    public BaseActivityContext(ActivityDef activityDef, ScopedCachingGeneratorSource scopedCachingGeneratorSource, ExecutionContext executionContext) {
        this.activityDef = activityDef;
        this.scopedCachingGeneratorSource = scopedCachingGeneratorSource;
        this.executionContext = executionContext;
    }

    @Override
    public ActivityDef getActivityDef() {
        return activityDef;
    }

    @Override
    public ScopedCachingGeneratorSource getActivityGeneratorSource() {
        return scopedCachingGeneratorSource;
    }

    @Override
    public ExecutionContext getExecutionContext() {
        return executionContext;
    }

}
