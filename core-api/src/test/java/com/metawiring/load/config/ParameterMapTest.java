package com.metawiring.load.config;

import org.testng.annotations.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

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
public class ParameterMapTest {

    @Test
    public void testNullStringYieldsNothing() {
        Optional<ParameterMap> parameterMap = ParameterMap.parseParams(null);
        assertThat(parameterMap.isPresent()).isFalse();
    }

    @Test
    public void testEmptyStringYieldsEmptyMap() {
        Optional<ParameterMap> parameterMap = ParameterMap.parseParams("");
        assertThat(parameterMap.isPresent()).isTrue();
    }

    @Test
    public void testUnparsableYieldsNothing() {
        Optional<ParameterMap> unparseable = ParameterMap.parseParams("woejfslkdjf");
        assertThat(unparseable.isPresent()).isFalse();

    }

    @Test
    public void testGetLongParam() {
        Optional<ParameterMap> longOnly = ParameterMap.parseParams("longval=234433;");
        assertThat(longOnly.isPresent()).isTrue();
        assertThat(longOnly.get().getLongOrDefault("longval", 12345l)).isEqualTo(234433l);
        assertThat(longOnly.get().getLongOrDefault("missing", 12345l)).isEqualTo(12345l);
    }

    @Test
    public void testGetDoubleParam() {
        Optional<ParameterMap> doubleOnly = ParameterMap.parseParams("doubleval=2.34433;");
        assertThat(doubleOnly.isPresent()).isTrue();
        assertThat(doubleOnly.get().getDoubleOrDefault("doubleval", 3.4567d)).isEqualTo(2.34433d);
        assertThat(doubleOnly.get().getDoubleOrDefault("missing", 3.4567d)).isEqualTo(3.4567d);
    }

    @Test
    public void testGetStringParam() {
        Optional<ParameterMap> stringOnly = ParameterMap.parseParams("stringval=avalue;");
        assertThat(stringOnly.isPresent()).isTrue();
        assertThat(stringOnly.get().getStringOrDefault("stringval", "othervalue")).isEqualTo("avalue");
        assertThat(stringOnly.get().getStringOrDefault("missing","othervalue")).isEqualTo("othervalue");

    }


}