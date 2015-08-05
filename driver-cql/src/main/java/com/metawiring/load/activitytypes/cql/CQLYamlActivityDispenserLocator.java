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

import com.metawiring.load.activity.ActivityDispenser;
import com.metawiring.load.activity.locators.YAMLResourceADL;
import com.metawiring.load.config.ActivityDef;

import java.io.InputStream;

public class CQLYamlActivityDispenserLocator extends YAMLResourceADL<CQLYamlActivity> {

    public CQLYamlActivityDispenserLocator(String matchingSuffix, String... searchPaths) {
        super(matchingSuffix, searchPaths);
    }

    @Override
    public ActivityDispenser<CQLYamlActivity> resolve(
            ActivityDef activityDef,
            String pathVariant,
            InputStream stream) {
        return new CQLYamlActivityDispenser(activityDef, pathVariant, stream);
    }
}