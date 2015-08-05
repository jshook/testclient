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
import com.metawiring.load.activity.YAMLFileADL;
import com.metawiring.load.activity.ActivityDispenser;
import com.metawiring.load.activity.ActivityDispenserLocator;
import com.metawiring.load.config.ActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static java.util.Arrays.asList;

/**
 * Resolve an activity dispenser which uses class path resources to find the YAML template.
 */
public abstract class  YAMLResourceADL<T extends Activity> implements ActivityDispenserLocator<T> {
    private final static Logger logger = LoggerFactory.getLogger(YAMLResourceADL.class);
    private String[] searchPaths = new String[0];
    private List<String> matchingSuffixes = new ArrayList<String>() {{
        add(".yaml");
    }};

    public YAMLResourceADL(String matchingSuffix, String... searchPaths) {
        this.searchPaths = searchPaths;
        matchingSuffixes.set(0,matchingSuffix);
    }

    public YAMLResourceADL withMatchingSuffixes(String... suffixes) {
        this.matchingSuffixes = asList(suffixes);
        return this;
    }

    @Override
    public Optional<ActivityDispenser<T>> resolve(ActivityDef activityDef) {

        for (String searchPath : searchPaths) {

            List<String> pathVariants = new ArrayList<>();
            matchingSuffixes.stream().map(s -> searchPath + File.separator + activityDef.getName() + s).forEach(pathVariants::add);

            for (String pathVariant : pathVariants) {
                logger.debug("Looking for " + pathVariant + " in class path.");
                try {
                    InputStream stream = YAMLFileADL.class.getClassLoader().getResourceAsStream(pathVariant);
                    if (stream != null) {
                        logger.info("Located " + activityDef.getName() + " in class path: " + pathVariant);
                        return Optional.of(resolve(activityDef,pathVariant,stream));
                    }
                } catch (Exception ignored) {
                }
            }
        }

        return Optional.empty();
    }

    public abstract ActivityDispenser<T> resolve(ActivityDef activityDef, String pathVariant, InputStream stream);

}
