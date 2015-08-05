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

package com.metawiring.load.activity;

//import com.metawiring.load.activitytypes.cql.CQLYamlActivity;

import com.metawiring.load.activitytypes.ActivityType;
import com.metawiring.load.config.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Iterate of available ways of finding activity types, given activityDefs
 */
public class ActivityTypeLocator {

    private final static Logger logger = LoggerFactory.getLogger(ActivityTypeLocator.class);
    private Map<ActivityType, ActivityDispenserLocator> typeLocatorMap;

    private List<ActivityDispenserLocator> activityDispenserLocators;


    public Optional<ActivityType> resolveActivityType(ActivityDef activityDef) {
        Map<ActivityType, ActivityDispenserLocator> typesToLocaters = getTypeLocaterMap();
        for (ActivityType activityType : typesToLocaters.keySet()) {
            ActivityDispenserLocator activityDispenserLocator = typesToLocaters.get(activityType);
            Optional activityDispenser = activityDispenserLocator.resolve(activityDef);
            if (activityDispenser.isPresent()) {
                return Optional.of(activityType);
            }
        }
        return Optional.empty();
    }

    private Map<ActivityType,ActivityDispenserLocator> getTypeLocaterMap() {
        if (typeLocatorMap == null) {
            synchronized (this) {
                if (typeLocatorMap == null) {
                    ServiceLoader<ActivityType> activityTypeServiceLoader =
                            ServiceLoader.load(ActivityType.class);
                    typeLocatorMap = new LinkedHashMap<>();
                    activityTypeServiceLoader.forEach(sl -> typeLocatorMap.put(sl, sl.getActivityDispenserLocator()));
                }
            }
        }
        return typeLocatorMap;
    }

}
