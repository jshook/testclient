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
package com.metawiring.load.config;

import java.util.*;
import java.util.regex.Pattern;

public class PhaseDef {
    private String name;
    private Map<String,ActivityDef> activityDefs = new LinkedHashMap<>();

    public PhaseDef(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }


    public void addActivityDef(ActivityDef activityDef) {
        if (activityDefs.containsKey(activityDef.getName())) {
            throw new RuntimeException(
                    "activity named '" + activityDef.getName() +"' already exists in phase named '" + getName() +'"'
            );
        }
        activityDefs.put(activityDef.getName(),activityDef);
    }

    public void addActivitySpec(String activitySpec) {
        ActivityDef activityDef = ActivityDef.parseActivityDef(activitySpec);
        addActivityDef(activityDef);
    }


    public List<ActivityDef> getActivityDefs() {
        return new ArrayList<ActivityDef>(activityDefs.values());
    }

    private static Pattern pattern = Pattern.compile("^([a-zA-Z0-9_-]+)$");

    public static PhaseDef parsePhaseDef(String phaseDefSpec) {
        if(pattern.matcher(phaseDefSpec).matches()) {
            return new PhaseDef(phaseDefSpec);
        } else {
            throw new RuntimeException("phase name: " + phaseDefSpec + " could not be parsed. Phase names are simple words, hyphens and underscores");
        }
    }

    public static Optional<PhaseDef> parsePhaseDefOptionally(String phaseDefSpec) {
        try {
            return Optional.of(parsePhaseDef(phaseDefSpec));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

}
