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

import com.metawiring.load.activities.YamlConfigurableActivity;
import com.metawiring.load.activities.WriteTelemetryAsyncActivity;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.YamlActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DefaultActivitySourceResolver implements ActivitySourceResolver {
    private final static Logger logger = LoggerFactory.getLogger(DefaultActivitySourceResolver.class);

    private List<String> activitySearchPackages = new ArrayList<String>() {{
        add(WriteTelemetryAsyncActivity.class.getPackage().getName());
    }};

    private Map<ActivityDef, ActivityInstanceSource> activityInstanceSourceMap = new HashMap<>();

    @Override
    public synchronized ActivityInstanceSource get(ActivityDef activityDef) {
        ActivityInstanceSource activitySource = activityInstanceSourceMap.get(activityDef);
        if (activitySource != null) {
            return activitySource;
        }

        Class<? extends Activity> activityClass = findActivityClass(activityDef);
        activitySource = new DefaultActivityInstanceSource(activityClass, activityDef.getName());
        activityInstanceSourceMap.put(activityDef, activitySource);
        return activitySource;
    }

    private Class<? extends Activity> findActivityClass(ActivityDef activityDef) {
        Class<? extends Activity> activityClass = null;
        activityClass = findDirectActivityClass(activityDef);
        if (activityClass != null) {
            return activityClass;
        }
        activityClass = findYamlActivityClass(activityDef);
        if (activityClass != null) {
            return activityClass;
        }
        throw new RuntimeException("Unable to find any type of activity class for " + activityDef.toString());
    }


    @SuppressWarnings("unchecked")
    private Class<Activity> findDirectActivityClass(ActivityDef activityDef) {
        if (activityDef.getName().contains(".")) {
            try {
                return (Class<Activity>) Class.forName(activityDef.getName());
            } catch (ClassNotFoundException e) {
                logger.error("Could not find class: " + activityDef.getName());
                throw new RuntimeException(e);
            }
        }

        Class<Activity> foundClass = null;
        LinkedList<String> triedLocations = new LinkedList<>();

        for (String packageName : activitySearchPackages) {
            triedLocations.add(packageName + "." + activityDef.getName());
            triedLocations.add(packageName + "." + activityDef.getName() + "Activity");
        }
        for (String location : triedLocations) {
            try {
                foundClass = (Class<Activity>) Class.forName(location);
                if (foundClass != null) {
                    break;
                }
            } catch (ClassNotFoundException ignored) {
            }

        }
        return foundClass;
    }

    private Class<? extends Activity> findYamlActivityClass(ActivityDef activityDef) {
        Class<? extends Activity> foundClass = null;

        for (String name : new String[]{"activities/" + activityDef.getName(), "activities/" + activityDef.getName() + ".yaml"}) {

            logger.debug("Looking for " + name + " on filesystem.");
            if (new File(name).exists()) {
                return YamlConfigurableActivity.class;
            } else {
                logger.info("Activity '" + activityDef.getName() + "' not found in '" + name + "' (filesystem)...");
            }


            logger.debug("Looking for " + name + " on in classpath.");
            try {
                InputStream stream = DefaultActivitySourceResolver.class.getClassLoader().getResourceAsStream(name);
                if (stream != null) {
                    return YamlConfigurableActivity.class;
                } else {
                    logger.info("Activity '" + activityDef.getName() + "' not found in '" + name + "' (classpath)...");
                }
            } catch (Exception e) {
                logger.warn("Unable to find yaml class for:" + activityDef.toString());
            }

        }
        return null;
    }

}
