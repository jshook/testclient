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

package com.metawiring.load.activity.dispensers;

import com.metawiring.load.activities.oldcql.CQLYamlActivity;
import com.metawiring.load.activity.ActivityDispenser;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.YamlActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Dispense activities based on the provided YAML File
 */
public class YamlActivityDispenser implements ActivityDispenser<CQLYamlActivity> {
    private static final Logger logger = LoggerFactory.getLogger(YamlActivityDispenser.class);
    private Yaml yaml = new Yaml();

    private final ActivityDef actityDef;
    private final String fromPath;
    private YamlActivityDef yamlActivityDef;
    private String name;

    /**
     * @param activityDef - source activity def for this activity dispenser
     * @param fromPath - the logical path of the YAML file
     * @param stream - an optional input stream. If passed, will be used instead of opening the path. This allows
     * for easy resource loading.
     */
    public YamlActivityDispenser(ActivityDef activityDef, String fromPath, InputStream stream) {
        this.actityDef = activityDef;
        this.fromPath = fromPath;
        yamlActivityDef=loadYaml(fromPath,stream);
        // TODO: Fix GI
    }

    public String toString() {
        return "activityDef:" + actityDef + " fromPath:" + fromPath;
    }

    private YamlActivityDef loadYaml(String fromPath, InputStream stream) {
        if (stream==null) {
            try {
                stream = new FileInputStream(fromPath);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Missing " + fromPath + " while trying to create dispenser.");
            }
        }

        try {
            return yaml.loadAs(stream, YamlActivityDef.class);
        } catch (Exception e) {
            logger.error("Error loading yaml from " + fromPath, e);
            throw e;
        }
    }

    @Override
    public CQLYamlActivity getNewInstance() {
        return new CQLYamlActivity(yamlActivityDef);
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public String getActivityName() {
        return null;
    }
}
