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

// import com.datastax.driver.core.ConsistencyLevel;

// import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

/**
 * Everything in the config is
 */
public class TestClientConfig {

    public final String host;
    public final int port;
    public final List<PhaseDef> phaseDefs;
    //    public final List<ActivityDef> activitytypes;
    public final String graphiteHost, metricsPrefix;
    public final int graphitePort;
    public final String keyspace;
    public final String table;
    public final boolean createSchema;
    public final String user;
    public final String password;
//    public final ConsistencyLevel defaultConsistencyLevel;
    public final int defaultReplicationFactor;
    public final boolean splitCycles;
    public final boolean diagnoseExceptions;

    private TestClientConfig(
            String host, int port,
            List<PhaseDef> phaseDefs,
            String metricsPrefix,
            String graphiteHost,
            int graphitePort,
            int verbosity,
            String keyspace,
            String table,
            boolean createSchema,
            String user,
            String password,
            int defaultReplicationFactor,
//            ConsistencyLevel defaultConsistencyLevel,
            boolean splitCycles,
            boolean diagnoseExceptions) {
        this.host = host;
        this.port = port;
        this.metricsPrefix = metricsPrefix;
        this.graphiteHost = graphiteHost;
        this.graphitePort = graphitePort;
        this.keyspace = keyspace;
        this.table = table;
        this.createSchema = createSchema;
        this.user = user;
        this.password = password;
//        this.defaultConsistencyLevel = defaultConsistencyLevel;
        this.defaultReplicationFactor = defaultReplicationFactor;
        this.splitCycles = splitCycles;
        this.diagnoseExceptions = diagnoseExceptions;
        this.phaseDefs = phaseDefs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<PhaseDef> getPhaseDefs() {
        return phaseDefs;
    }

    public static class Builder {

        private String host = "localhost";
        private int port = 9042;
        private List<PhaseDef> phaseDefs;
        private String metricsPrefix = "";
        private String graphiteHost;
        private int graphitePort;
        private int verbosity = 0;
        private String keyspace = "test";
        private String table = "test";
        private boolean createSchema = false;
        private String user;
        private String password;
//        private ConsistencyLevel defaultConsistencyLevel = ConsistencyLevel.ONE;
        private int defaultReplicationFactor = 1;
        private boolean splitCycles = true;
        private boolean diagnoseExceptions = false;
//        private int replicationFactor = 1;

        public TestClientConfig build() {
            return new TestClientConfig(
                    host,
                    port,
                    phaseDefs,
                    metricsPrefix,
                    graphiteHost,
                    graphitePort,
                    verbosity,
                    keyspace,
                    table,
                    createSchema,
                    user,
                    password,
                    defaultReplicationFactor,
//                    defaultConsistencyLevel,
                    splitCycles,
                    diagnoseExceptions);
        }

        // TODO: add activity-type properties, instance and global
        public Builder withCredentials(String user, String password) {
            this.user = user;
            this.password = password;
            return this;
        }

        public Builder withHost(String host) {
            this.host = host;
            return this;
        }

        public Builder withPort(int port) {
            this.port = port;
            return this;
        }

        public Builder withSplitCycles(boolean splitCycles) {
            this.splitCycles = splitCycles;
            return this;
        }

        public Builder withDiagnoseExceptions(boolean diagnoseExceptions) {
            this.diagnoseExceptions = diagnoseExceptions;
            return this;
        }

        public Builder withPhaseAndActivityDefs(List<String> strings) {
            LinkedList<PhaseDef> phaseDefList = new LinkedList<>();
            phaseDefList.add(new PhaseDef("default"));

            for (String string : strings) {
                Optional<PhaseDef> phaseDef = PhaseDef.parsePhaseDefOptionally(string);
                Optional<ActivityDef> activityDef = ActivityDef.parseActivityDefOptionally(string);
                if (phaseDef.isPresent()) {
                    phaseDefList.add(phaseDef.get());
                } else if (activityDef.isPresent()) {
                    phaseDefList.peekLast().addActivityDef(activityDef.get());
                } else {
                    throw new RuntimeException("Not able to parse " + string + " as either a phase name or an activity def");
                }
            }
            LinkedList<PhaseDef> filteredPhaseDefs = new LinkedList<PhaseDef>();
            phaseDefList.stream().filter(pd -> pd.getActivityDefs().size() > 0).forEach(filteredPhaseDefs::add);
            phaseDefs = filteredPhaseDefs;
            return this;
        }

        public Builder withGraphite(String graphite) {
            this.graphiteHost = graphite.split(":")[0];
            this.graphitePort = graphite.contains(":") ? Integer.valueOf(graphite.split(":")[1]) : 2003;
            return this;
        }

        public Builder withMetricsPrefix(String metricsPrefix) {
            this.metricsPrefix = metricsPrefix;
            return this;
        }

        public Builder withKeyspace(String keyspace) {
            this.keyspace = keyspace;
            return this;
        }

        public Builder withTable(String table) {
            this.table = table;
            return this;
        }

        public Builder withCreateSchema(Boolean createSchema) {
            this.createSchema = createSchema;
            return this;
        }

//        public Builder withDefaultConsistencyLevel(String cl) {
//            this.defaultConsistencyLevel = ConsistencyLevel.valueOf(cl);
//            return this;
//        }

        public Builder withDefaultReplicationFactor(String rf) {
            this.defaultReplicationFactor = Integer.valueOf(rf);
            return this;
        }

    }
}
