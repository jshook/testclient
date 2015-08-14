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

package com.metawiring.load.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A definition for an activity.
 */
public class ActivityDef {
    private final static Logger logger = LoggerFactory.getLogger(ActivityDef.class);

    private String name;
    private String source;

    private long startCycle = 1;
    private long endCycle = 1;
    private int threads = 1;
    private int maxAsync = 1000;
    private int interCycleDelay = 0;
    private ParameterMap paramMap;

    public ActivityDef(
            String name,
            String source,
            long startCycle,
            long endCycle,
            int threads,
            int maxAsync,
            int interCycleDelay,
            Optional<ParameterMap> parameterMap) {
        this.name = name;
        this.source = source;
        this.threads = threads;
        this.maxAsync = maxAsync;
        this.startCycle = startCycle;
        this.endCycle = endCycle;
        this.interCycleDelay = interCycleDelay;
        this.paramMap = parameterMap.orElse(null);
    }

    public ActivityDef(ParameterMap parameterMap) {
        this(
                parameterMap.getStringOrDefault("name", "unnamed-activity"),
                parameterMap.getStringOrDefault("source", "unknown-source"),
                parameterMap.getLongOrDefault("startcycle", 1l),
                parameterMap.getLongOrDefault("endcycle", 1l),
                parameterMap.getIntOrDefault("threads", 1),
                parameterMap.getIntOrDefault("async",1),
                parameterMap.getIntOrDefault("delay",0),
                Optional.ofNullable(parameterMap)
        );
    }

    /**
     * @param namedActivitySpec - namedActivitySpec name in one of the formats:
     * <UL>
     * <LI>activityClass</LI>
     * <LI>activityClass:cycles</LI>
     * <LI>activityClass:cycles:threads</LI>
     * <LI>activityClass:cycles:threads:maxAsync/LI>
     * </UL>
     * <p>where cycles may be either M or N..M</p>
     * <p>M implicitly represent 1..M</p>
     * <p>Any of these patterns may be followed or replace by a name=value;... form. They are internally anyway.</p>
     */

    // NOTE: To use this regex naming scheme, every group which is not named must be a non-capturing group, so
    // group names and capturing groups match up pair-wise. See the supporting getNamedGroupCandidates(...) helper method below.

    private static Pattern shortPattern =
            Pattern.compile(
                    "^"
                            + "(?<name>[a-zA-Z\\.-_]+)?:?"
                            + "(?<source>[a-zA-Z\\.-_\\\\]+)?:?"
                            + "(?<cyc>: (?: (?<startcycle>\\d+? ) (?:\\.\\.)? )? (?<endcycle>\\d+?) )?:?"
                            + "(?<threads>\\d+?)?:?"
                            + "(?<async>\\d+?)?:?"
                            + "(?<delay>\\d+?)?:?"
                            + "$", Pattern.COMMENTS
            );
    private static List<String> shortPatternNames = getNamedGroupCandidates(shortPattern.pattern());

    private static Pattern argsPattern =
            Pattern.compile(
                    "((\\w+?)=(.+?);)*", Pattern.COMMENTS
            );

    private static Pattern fullPattern =
            Pattern.compile(
                    "^"
                            + "(?<shortForm>" + shortPattern + ")?"
                            + "(?:[,:])?"
                            + "(?<argsForm>" + argsPattern + ")?"
                            + "$", Pattern.COMMENTS
            );
    private static List<String> fullPatternGroups = getNamedGroupCandidates(fullPattern.pattern());

    public static Optional<ActivityDef> parseActivityDefOptionally(String namedActivitySpec) {
        try {
            ActivityDef activityDef = parseActivityDef(namedActivitySpec);
            return Optional.of(activityDef);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static ActivityDef parseActivityDef(String namedActivitySpec) {

        Matcher fullMatcher = fullPattern.matcher(namedActivitySpec);

        if (!fullMatcher.matches()) {
            throw new RuntimeException("Unable to parse named activity spec:" + namedActivitySpec);
        }
//        else {
//            for (int i = 0; i < matcher.groupCount(); i++) {
//                logger.info("group(" + (i+1) + "/" + groupNames.get(i) + "):" + matcher.group(i+1) );
//            }
//            System.out.println("matched!");
//        }

        Optional<String> shortForm = Optional.ofNullable(fullMatcher.group("shortForm"));
        Optional<String> argsForm = Optional.ofNullable(fullMatcher.group("argsForm"));
        String allArgs = argsForm.orElse("");

        if (shortForm.isPresent()) {

            Matcher shortFormMatcher = shortPattern.matcher(shortForm.get());
            if (!shortFormMatcher.matches()) {
                throw new RuntimeException("Unable to parse named activity spec short form:" + shortForm.get());
            }

            allArgs +=
                    shortPatternNames.stream()
                            .filter(s -> Optional.ofNullable(shortFormMatcher.group(s)).isPresent())
                            .map(v -> v + "=" + shortFormMatcher.group(v) + ";")
                            .collect(Collectors.joining());
        }

        ParameterMap activityParameterMap = ParameterMap.parseOrException(allArgs);
        ActivityDef activityDef = new ActivityDef(activityParameterMap);
        return activityDef;

    }

    private static List<String> getNamedGroupCandidates(String regex) {
        List<String> namedGroups = new ArrayList<String>();

        Matcher m = Pattern.compile("\\(\\?<([a-zA-Z][a-zA-Z0-9]*)>").matcher(regex);

        while (m.find()) {
            namedGroups.add(m.group(1));
        }

        return namedGroups;
    }


    public String toString() {
        if (startCycle == 1) {
            return name + ":" + endCycle + ":" + threads + ":" + maxAsync;
        } else {
            return name + ":" + startCycle + "-" + endCycle + ":" + threads + ":" + maxAsync;
        }
    }

    public String getName() {
        return name;
    }

    public long getStartCycle() {
        return startCycle;
    }

    public long getEndCycle() {
        return endCycle;
    }

    /**
     * Returns the greater of threads or maxAsync. The reason for this is that maxAsync less than threads will starve
     * threads of async grants, since the async is apportioned to threads in an activity.
     *
     * @return maxAsync, or threads if threads is greater
     */
    public int getMaxAsync() {
        if (maxAsync < threads) return threads;
        else return maxAsync;
    }

    public int getThreads() {
        return threads;
    }

    public long getTotalCycles() {
        return endCycle - startCycle;
    }

    public int getInterCycleDelay() {
        return interCycleDelay;
    }

    public void setCycles(String cycles) {
        int rangeAt = cycles.indexOf("..");
        if (rangeAt > 0) {
            setStartCycle(Long.valueOf(cycles.substring(0, rangeAt)));
            setEndCycle(Long.valueOf(cycles.substring(rangeAt + 2)));
        } else {
            setStartCycle(1);
            setEndCycle(Long.valueOf(cycles));
        }
    }

    public void setStartCycle(long startCycle) {
        this.startCycle = startCycle;
    }

    public void setEndCycle(long endCycle) {
        this.endCycle = endCycle;
    }

    public ParameterMap getParams() {
        return paramMap;
    }

    public String getSource() {
        return source;
    }
}
