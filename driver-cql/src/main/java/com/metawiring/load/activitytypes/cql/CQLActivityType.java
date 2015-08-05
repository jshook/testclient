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
package com.metawiring.load.activitytypes.cql;

import com.metawiring.load.activity.ActivityDispenserLocator;
import com.metawiring.load.activity.FunctionalFactory;
import com.metawiring.load.activitytypes.ActivityType;
import com.metawiring.load.activity.ActivityDispenserFactory;
import com.metawiring.load.activitytypes.SharedActivityState;
import com.metawiring.load.activitytypes.SharedPhaseState;
import com.metawiring.load.config.ActivityDef;

public class CQLActivityType implements ActivityType<CQLYamlActivity,CQLSharedActivityContext> {

    @Override
    public ActivityDispenserLocator<CQLYamlActivity> getActivityDispenserLocator() {
        return new CQLYamlActivityDispenserLocator(".cql.yaml", "activities");
    }

    @Override
    public ActivityDispenserFactory<CQLYamlActivity> getActivityDispenserFactory() {
        return null;
    }

    @Override
    public CQLSharedActivityContext getSharedActivityState(SharedPhaseState sharedPhaseState,ActivityType at, ActivityDef ad) {
        return new CQLSharedActivityContext(sharedPhaseState,at,ad);
    }

}
