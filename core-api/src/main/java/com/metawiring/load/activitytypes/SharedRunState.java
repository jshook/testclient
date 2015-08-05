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
package com.metawiring.load.activitytypes;

import com.codahale.metrics.MetricRegistry;
import com.metawiring.load.activity.ActivityTypeLocator;
import com.metawiring.load.config.TestClientConfig;
import com.metawiring.load.core.MetricReporters;
import com.metawiring.load.generator.GeneratorInstantiator;
import com.metawiring.load.generator.RuntimeScope;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import com.metawiring.load.generator.ScopedGeneratorCache;

public class SharedRunState {

    private TestClientConfig config;
    private MetricRegistry metrics = new MetricRegistry();
    private MetricReporters metricReporters = MetricReporters.getInstance();
    private ScopedCachingGeneratorSource runScopedGeneratorSource;
    private ActivityTypeLocator activityTypeLocator = new ActivityTypeLocator();

    public SharedRunState() {

        this.runScopedGeneratorSource =
                new ScopedGeneratorCache(
                        new GeneratorInstantiator(), RuntimeScope.process
                );
    }

    public ScopedCachingGeneratorSource getRunScopedGeneratorSource() {
        return runScopedGeneratorSource;
    }

    public TestClientConfig getConfig() {
        return config;
    }

    public void setConfig(TestClientConfig config) {
        this.config = config;
    }

    public MetricRegistry getMetrics() {
        return metrics;
    }

    public void setMetrics(MetricRegistry metrics) {
        this.metrics = metrics;
        metricReporters.addRegistry(metrics);
        metricReporters.addLogger(metrics);
    }

    public MetricReporters getMetricReporters() {
        return metricReporters;
    }

    public ActivityTypeLocator getActivityTypeLocator() {
        return activityTypeLocator;
    }
}
