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
package com.metawiring.load.activities.cql;

import com.metawiring.load.cycler.api.ActionDispenser;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.api.ActivityAction;

public class CQLActionDispenser implements ActionDispenser {
    private final ActivityDef activityDef;

    public CQLActionDispenser(ActivityDef activityDef) {
        this.activityDef = activityDef;
    }

    @Override
    public ActivityAction getAction(int slot, ActivityDef activityDef) {
        return new ActivityAction() {

            private ActivityDef adef = activityDef;

            @Override
            public void accept(long value) {

            }
        };
    }
}
