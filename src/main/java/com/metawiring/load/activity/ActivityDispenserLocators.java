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

import com.metawiring.load.activities.WriteTelemetryAsyncActivity;
import com.metawiring.load.activity.locators.*;
import com.metawiring.load.config.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Iterate of available ways of finding activity dispensers, hopefully to find one for the given name.
 */
public class ActivityDispenserLocators implements ActivityDispenserFactory {
    private final static Logger logger = LoggerFactory.getLogger(ActivityDispenserLocators.class);

    // This static initializer controls the search precedence for different activity locators and everything
    // that follows.
    private final List<ActivityDispenserLocator> activityDispenserLocators = new ArrayList<ActivityDispenserLocator>() {{
        add(new YAMLFileADL("activities"));
        add(new YAMLResourceADL("activities"));
        add(new DirectImplADL(
                WriteTelemetryAsyncActivity.class.getPackage().getName()
        ));
    }};


    private Map<ActivityDef, ActivityDispenser> activityInstanceSourceMap = new HashMap<>();

    @Override
    public synchronized ActivityDispenser get(ActivityDef activityDef) {
        ActivityDispenser activityDispenser = activityInstanceSourceMap.get(activityDef);
        if (activityDispenser != null) {
            return activityDispenser;
        }

        Optional<? extends ActivityDispenser> resolvedActivityDispenser = Optional.empty();
        for (ActivityDispenserLocator activityDispenserLocator : activityDispenserLocators) {
            resolvedActivityDispenser = activityDispenserLocator.resolve(activityDef);
            if (resolvedActivityDispenser.isPresent()) {
                activityDispenser = resolvedActivityDispenser.get();
                activityInstanceSourceMap.put(activityDef,activityDispenser);
                return activityDispenser;
            }
        }

        throw new RuntimeException("Could not resolve activity dispenser for " + activityDef.getAlias());

    }



}
