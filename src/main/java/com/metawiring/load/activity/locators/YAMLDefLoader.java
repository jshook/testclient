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

import com.metawiring.load.activity.ActivityDispenser;
import com.metawiring.load.activity.ActivityDispenserLocator;
import com.metawiring.load.activity.dispensers.YamlActivityDispenser;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.YamlActivityDef;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Optional;

/**
 * Resolve an activity dispenser which uses class path resources to find the YAML template.
 */
public class YAMLDefLoader<T> {
    private final static Logger logger = LoggerFactory.getLogger(YAMLDefLoader.class);
    private String[] searchPaths = new String[0];

    public YAMLDefLoader(String... searchPaths) {
        this.searchPaths = searchPaths;
    }

    public Optional<T> load(String resourceName) {
        for (String searchPath : searchPaths) {
            String[] pathVariants = new String[]{
                    searchPath + File.separator + resourceName,
                    searchPath + File.separator + resourceName + ".yaml"
            };

            for (String pathVariant : pathVariants) {
                logger.debug("Looking for " + pathVariant + " in class path.");
                try {
                    InputStream stream = YAMLFileADL.class.getClassLoader().getResourceAsStream(pathVariant);
                    if (stream != null) {
                        logger.info("Located " + resourceName + " in class path: " + pathVariant);
                        return Optional.ofNullable(loadYaml(resourceName,stream));
                    }
                } catch (Exception e) {
                    throw new RuntimeException("error loading " + resourceName, e);
                }
            }

        }
    }

    private T loadYaml(String fromPath, InputStream stream) {

        Yaml yaml = new Yaml();
        if (stream==null) {
            try {
                stream = new FileInputStream(fromPath);
            } catch (FileNotFoundException e) {
                throw new RuntimeException("Missing " + fromPath + " while trying to create dispenser.");
            }
        }

        // TODO: Encode the class type in the loader calling signature
        try {
            return yaml.loadAs(stream, T);
        } catch (Exception e) {
            logger.error("Error loading yaml from " + fromPath, e);
            throw e;
        }
    }


}
