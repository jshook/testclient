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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    /**
     * @param namedActivitySpec - namedActivitySpec name in one of the formats:
     * <UL>
     * <LI>activityClass</LI>
     * <LI>activityClass:cycles</LI>
     * <LI>activityClass:cycles:threads</LI>
     * <LI>activityClass:cycles:threads:maxAsync/LI>
     * </UL>
     * where cycles may be either M or N..M
     * N implicitly represent 1..M
     */

    // NOTE: To use this regex naming scheme, every group which is not named must be a non-capturing group, so
    // group names and capturing groups match up pair-wise. See the supporting getNamedGroupCandidates(...) helper method below.

    private static Pattern argPattern =
            Pattern.compile(
                    "(\\w+?)=(.+?);", Pattern.COMMENTS
            );
    private static Pattern argsPattern =
            Pattern.compile("(?<args>(" + argPattern + ")+)?");


    private static Pattern activityPattern =
            Pattern.compile(
                    "^"
                            + "(?<names> (?: (?<name>[a-zA-Z\\.\\-]*?) (?::)? ) (?<source>\\w*?)) "
                            + "(?<cyc>: (?: (?<cstart>\\d+? ) (?:\\.\\.)? )? (?<cstop>\\d+?) )?"
                            + "(?: (?::) (?<threads>\\d+?) )?"
                            + "(?: (?::) (?<async>\\d+?) )?"
                            + "(?: (?::) (?<delay>\\d+?) )?"
                            + "(?: (?:[,:]) " + argsPattern + ")?"
                            + "$", Pattern.COMMENTS
            );


    private static List<String> groupNames = getNamedGroupCandidates(activityPattern.pattern());

    public static Optional<ActivityDef> parseActivityDefOptionally(String namedActivitySpec) {
        try {
            ActivityDef activityDef = parseActivityDef(namedActivitySpec);
            return Optional.of(activityDef);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static ActivityDef parseActivityDef(String namedActivitySpec) {

        Matcher matcher = activityPattern.matcher(namedActivitySpec);
        if (!matcher.matches()) {
            throw new RuntimeException("Unable to parse named activity spec:" + namedActivitySpec);
        }
//        else {
//            for (int i = 0; i < matcher.groupCount(); i++) {
//                logger.info("group(" + (i+1) + "/" + groupNames.get(i) + "):" + matcher.group(i+1) );
//            }
//            System.out.println("matched!");
//        }

        Optional<String> name = Optional.ofNullable(matcher.group("name"));
        Optional<String> source = Optional.ofNullable(matcher.group("source"));
        Optional<String> cstart = Optional.ofNullable(matcher.group("cstart"));
        Optional<String> cstop = Optional.ofNullable(matcher.group("cstop"));
        Optional<String> threads = Optional.ofNullable(matcher.group("threads"));
        Optional<String> async = Optional.ofNullable(matcher.group("async"));
        Optional<String> delay = Optional.ofNullable(matcher.group("delay"));
        Optional<String> args = Optional.ofNullable(matcher.group("args"));

        Optional<ParameterMap> optionalParameterMap = ParameterMap.parseOptionalParams(args);

        ActivityDef activityDef = new ActivityDef(
                name.orElse("unnamed-activity"),
                source.orElse(name.orElse("unnamed-source")),
                Long.valueOf(cstart.orElse("1")),
                Long.valueOf(cstop.orElse("1")),
                Integer.valueOf(threads.orElse("1")),
                Integer.valueOf(async.orElse("1")),
                Integer.valueOf(delay.orElse("0")),
                optionalParameterMap
        );
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
