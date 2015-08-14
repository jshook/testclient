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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParameterMap {

    private Map<String, String> paramMap;

    private ParameterMap(Map<String, String> valueMap) {
        this.paramMap = valueMap;
    }

    public Long getLongOrDefault(String paramName, Long defaultLongValue) {
        Optional<String> l = Optional.ofNullable(paramMap.get(paramName));
        if (l.isPresent()) {
            return Long.valueOf(l.get());
        }
        return defaultLongValue;
    }
    public Long takeLongOrDefault(String paramName, Long defaultLongValue) {
        Optional<String> l = Optional.ofNullable(paramMap.get(paramName));
        if (l.isPresent()) {
            paramMap.remove(paramName);
            return Long.valueOf(l.get());
        }
        return defaultLongValue;
    }

    public Double getDoubleOrDefault(String paramName, double defaultDoubleValue) {
        Optional<String> d = Optional.ofNullable(paramMap.get(paramName));
        if (d.isPresent()) {
            return Double.valueOf(d.get());
        }
        return defaultDoubleValue;
    }
    public Double takeDoubleOrDefault(String paramName, double defaultDoubleValue) {
        Optional<String> d = Optional.ofNullable(paramMap.get(paramName));
        if (d.isPresent()) {
            paramMap.remove(paramName);
            return Double.valueOf(d.get());
        }
        return defaultDoubleValue;
    }

    public String getStringOrDefault(String paramName, String defaultStringValue) {
        Optional<String> s = Optional.ofNullable(paramMap.get(paramName));
        return s.orElse(defaultStringValue);
    }
    public String takeStringOrDefault(String paramName, String defaultStringValue) {
        Optional<String> s = Optional.ofNullable(paramMap.get(paramName));
        if (s.isPresent()) { paramMap.remove(paramName); }
        return s.orElse(defaultStringValue);
    }

    public int getIntOrDefault(String paramName, int paramDefault) {
        Optional<String> i = Optional.ofNullable(paramMap.get(paramName));
        if (i.isPresent()) {
            return Integer.valueOf(i.get());
        }
        return paramDefault;
    }
    public int takeIntOrDefault(String paramName, int paramDefault) {
        Optional<String> i = Optional.ofNullable(paramMap.get(paramName));
        if (i.isPresent()) {
            paramMap.remove(paramName);
            return Integer.valueOf(i.get());
        }
        return paramDefault;
    }


    private static Pattern encodedParamsPattern = Pattern.compile("(\\w+?)=(.+);");

    public static ParameterMap parseOrException(String encodedParams) {
        if (encodedParams == null) {
            throw new RuntimeException("Must provide a non-null String to parse parameters.");
        }

        Matcher matcher = encodedParamsPattern.matcher(encodedParams);

        LinkedHashMap<String, String> newParamMap = new LinkedHashMap<>();

        int lastEnd = 0;

        while (matcher.find()) {
            String paramName = matcher.group(1);
            String paramValueString = matcher.group(2);
            newParamMap.put(paramName, paramValueString);
            lastEnd = matcher.end();
        }

        if (lastEnd != encodedParams.length()) {
            throw new RuntimeException("unable to find pattern " + encodedParamsPattern.pattern() + " in input" + encodedParams);
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


    public int size() {
        return paramMap.size();
    }

}