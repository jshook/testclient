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

import com.metawiring.load.config.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Optional;

public abstract class YAMLFileADL<A extends Activity> implements ActivityDispenserLocator<A> {
    private static Logger logger = LoggerFactory.getLogger(YAMLFileADL.class);
    private String[] searchPaths = new String[0];

    public YAMLFileADL(String... searchPaths) {
        this.searchPaths = searchPaths;
    }

    @Override
    public Optional<ActivityDispenser<A>> resolve(ActivityDef activityDef) {

        for (String searchPath : searchPaths) {
            String[] pathVariants = new String[]{
                    searchPath + File.separator + activityDef.getName(),
                    searchPath + File.separator + activityDef.getName() + ".yaml"
            };

            for (String pathVariant : pathVariants) {
                logger.debug("Looking for " + pathVariant + " in filesystem.");
                File yamlFile = new File(pathVariant);
                if (yamlFile.exists()) {
                    return Optional.of(resolve(activityDef,pathVariant));
//                    return Optional.of(new CQLYamlActivityDispenser(activityDef,pathVariant,null));
                }
            }
        }

        return Optional.empty();
    }

    protected abstract ActivityDispenser<A> resolve(ActivityDef activityDef, String pathVariant);
}
