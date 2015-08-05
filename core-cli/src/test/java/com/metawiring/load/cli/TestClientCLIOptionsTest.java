package com.metawiring.load.cli;

import com.metawiring.load.config.TestClientConfig;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.*;

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
public class TestClientCLIOptionsTest {

    @Test
    public void testParsePhaseAndActivity() throws Exception {
        String[] cmdline1 = new String[]{"--diagnose", "--phase=phase_1", "--activity=test:testsource"};
        TestClientCLIOptions tcco = new TestClientCLIOptions();
        TestClientConfig parsed = tcco.parse(cmdline1);
        assertThat(parsed.getPhaseDefs().size()).isEqualTo(1);
    }

    @Test
    public void testParseDefaultPhase() throws Exception {
        String[] cmdline1 = new String[]{"--diagnose", "--activity=test:testsource"};
        TestClientCLIOptions tcco = new TestClientCLIOptions();
        TestClientConfig parsed = tcco.parse(cmdline1);
        assertThat(parsed.getPhaseDefs().size()).isEqualTo(1);
    }

    @Test
    public void testParseActivityParams() {
        String[] cmdline1 = new String[]{"--diagnose", "--activity=test:testsource,keyspace=fookeyspace;"};
        TestClientCLIOptions tcco = new TestClientCLIOptions();
        TestClientConfig parsed = tcco.parse(cmdline1);
        assertThat(parsed.getPhaseDefs().get(0).getActivityDefs().get(0).getParams().size()).isGreaterThan(0);
    }

}