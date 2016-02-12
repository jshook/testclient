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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * <p>A concurrently accessible parameter map which holds both keys and values as strings.
 * An atomic change counter tracks updates, to allow interested consumers to determine
 * when to re-read values across threads. The basic format is
 * &lt;paramname&gt;=&lt;paramvalue&gt;;...</p>
 *
 * <p>To create a parameter map, use one of the static parse... methods.</p>
 *
 * <p>No native types are used internally. Everything is encoded as a String.</p>
 */
public class ParameterMap {

    private final ConcurrentHashMap<String, String> paramMap = new ConcurrentHashMap<>(10);
    private final AtomicLong changeCounter = new AtomicLong(0L);

    private ParameterMap(Map<String, String> valueMap) {
        paramMap.putAll(valueMap);
    }

    public long getLongOrDefault(String paramName, long defaultLongValue) {
        Optional<String> l = Optional.ofNullable(paramMap.get(paramName));
        return l.map(Long::valueOf).orElse(defaultLongValue);
    }

    public double getDoubleOrDefault(String paramName, double defaultDoubleValue) {
        Optional<String> d = Optional.ofNullable(paramMap.get(paramName));
        return d.map(Double::valueOf).orElse(defaultDoubleValue);
    }

    public String getStringOrDefault(String paramName, String defaultStringValue) {
        Optional<String> s = Optional.ofNullable(paramMap.get(paramName));
        return s.orElse(defaultStringValue);
    }

    public int getIntOrDefault(String paramName, int defaultIntValue) {
        Optional<String> i = Optional.ofNullable(paramMap.get(paramName));
        return i.map(Integer::valueOf).orElse(defaultIntValue);
    }


    public Long takeLongOrDefault(String paramName, Long defaultLongValue) {
        Optional<String> l = Optional.ofNullable(paramMap.remove(paramName));
        Long lval = l.map(Long::valueOf).orElse(defaultLongValue);
        markMutation();
        return lval;
    }
    public Double takeDoubleOrDefault(String paramName, double defaultDoubleValue) {
        Optional<String> d = Optional.ofNullable(paramMap.remove(paramName));
        Double dval = d.map(Double::valueOf).orElse(defaultDoubleValue);
        markMutation();
        return dval;
    }
    public String takeStringOrDefault(String paramName, String defaultStringValue) {
        Optional<String> s = Optional.ofNullable(paramMap.remove(paramName));
        String sval = s.orElse(defaultStringValue);
        markMutation();
        return sval;
    }
    public int takeIntOrDefault(String paramName, int paramDefault) {
        Optional<String> i = Optional.ofNullable(paramMap.remove(paramName));
        int ival = i.map(Integer::valueOf).orElse(paramDefault);
        markMutation();
        return ival;
    }

    public void set(String paramName, Object newValue) {
        paramMap.put(paramName,String.valueOf(newValue));
        markMutation();
    }

    private static Pattern encodedParamsPattern = Pattern.compile("(\\w+?)=(.+?);");

    public static ParameterMap parseOrException(String encodedParams) {
        if (encodedParams == null) {
            throw new RuntimeException("Must provide a non-null String to parse parameters.");
        }

        Matcher matcher = encodedParamsPattern.matcher(encodedParams);

        LinkedHashMap<String, String> newParamMap = new LinkedHashMap<>();

        int lastEnd = 0;
        int triedAt = 0;

        while (matcher.find()) {
            triedAt = lastEnd;
            String paramName = matcher.group(1);
            String paramValueString = matcher.group(2);
            newParamMap.put(paramName, paramValueString);
            lastEnd = matcher.end();
        }

        if (lastEnd != encodedParams.length()) {
            throw new RuntimeException("unable to find pattern " + encodedParamsPattern.pattern() + " at position " + triedAt + " in input" + encodedParams);
        }

        return new ParameterMap(newParamMap);
    }

    public static Optional<ParameterMap> parseOptionalParams(Optional<String> optionalEncodedParams) {
        if (optionalEncodedParams.isPresent()) {
            return parseParams(optionalEncodedParams.get());
        }
        return Optional.empty();
    }

    public static Optional<ParameterMap> parseParams(String encodedParams) {
        try {
            return Optional.ofNullable(parseOrException(encodedParams));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Parse positional parameters, each suffixed with the ';' terminator.
     * @param encodedParams parameter string
     * @param fieldEnums values() of enums for mapping ordinals to field name values
     * @return a new ParameterMap, if parsing was successful
     */
    public static ParameterMap parsePositional(String encodedParams, String[] fieldEnums) {

        String[] splitAtSemi = encodedParams.split(";");
        if (splitAtSemi.length > fieldEnums.length) {
            throw new RuntimeException("positional param values exceed number of named fields:"
                    + " names:" + Arrays.toString(fieldEnums)
                    + ", values: " + Arrays.toString(splitAtSemi));
        }

        for (int wordidx = 0; wordidx < splitAtSemi.length; wordidx++) {

            if (!splitAtSemi[wordidx].contains("=")) {
                splitAtSemi[wordidx] = fieldEnums[wordidx] + "=" + splitAtSemi[wordidx] +";";
            }
            if (!splitAtSemi[wordidx].endsWith(";")) {
                splitAtSemi[wordidx] = splitAtSemi[wordidx] + ";";
            }
        }

        String allArgs = Arrays.asList(splitAtSemi).stream().collect(Collectors.joining());
        ParameterMap parameterMap = ParameterMap.parseOrException(allArgs);
        return parameterMap;
    }

    public int size() {
        return paramMap.size();
    }

    public int getSize() {
        return this.paramMap.size();
    }

    private void markMutation() {
        changeCounter.incrementAndGet();
    }

    public AtomicLong getChangeCounter() {
        return changeCounter;
    }


}