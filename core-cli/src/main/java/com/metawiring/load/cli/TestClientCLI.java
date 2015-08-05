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

package com.metawiring.load.cli;

import com.metawiring.load.config.TestClientConfig;
import com.metawiring.load.core.MetricReporters;
import org.slf4j.Logger;

import java.util.concurrent.Callable;

public class TestClientCLI implements Callable<MetricReporters>  {
    private static Logger logger = org.slf4j.LoggerFactory.getLogger(TestClientCLI.class);
    private TestClientConfig config;

    public static void main( String[] args ) throws Exception {
        TestClientConfig config = new TestClientCLIOptions().parse(args);
        TestClientCLI client = new TestClientCLI(config);
        MetricReporters reporters = client.call();
        reporters.reportTo(System.out);
    }

    public TestClientCLI(TestClientConfig config) {
        this.config = config;
    }

    @Override
    public MetricReporters call() throws Exception {
        return null;
    }
}
