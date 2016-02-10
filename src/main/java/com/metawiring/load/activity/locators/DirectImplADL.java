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

package com.metawiring.load.activity.locators;

import com.metawiring.load.activity.Activity;
import com.metawiring.load.activity.ActivityDispenser;
import com.metawiring.load.activity.ActivityDispenserLocator;
import com.metawiring.load.activity.dispensers.DirectImplActivityDispenser;
import com.metawiring.load.config.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Find a way to create an activity dispensor, if possible, from class activityDef.name.
 */
public class DirectImplADL implements ActivityDispenserLocator {
    private static final Logger logger = LoggerFactory.getLogger(DirectImplADL.class);
    private String[] activitySearchPackages = new String[0];

    public DirectImplADL(String... activitySearchPackages) {
        this.activitySearchPackages = activitySearchPackages;
    }

    @Override
    public Optional<? extends ActivityDispenser> resolve(ActivityDef activityDef) {

        List<String> searchPaths = new ArrayList<>();
        if (activityDef.getName().contains(".")) {
            searchPaths.add(activityDef.getName());
        } else {
            for (String activitySearchPackage : activitySearchPackages) {
                searchPaths.add(activitySearchPackage + "." + activityDef.getName());
                searchPaths.add(activitySearchPackage + "." + activityDef.getName() + "Activity");
            }
        }

        for (String searchPath : searchPaths) {
            try {
                Class<? extends Activity> activityClass = (Class<? extends Activity>) Class.forName(searchPath);
                if (activityClass != null) {
                    DirectImplActivityDispenser directImplActivityDispenser = new DirectImplActivityDispenser(activityDef.getName(), activityClass);
                    return Optional.of(directImplActivityDispenser);
                }
            } catch (ClassNotFoundException e) {
                logger.error("Could not find class: " + activityDef.getName());
            }

        }
        return Optional.empty();
    }


}
