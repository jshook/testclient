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
package com.metawiring.load.activitytypes.cql;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ProtocolOptions;
import com.datastax.driver.core.Session;
import com.metawiring.load.activitytypes.ActivityTypeState;
import com.metawiring.load.activitytypes.SharedRunState;
import com.metawiring.load.config.TestClientConfig;

public class CQLGlobalState implements ActivityTypeState {

    private final SharedRunState sharedRunState;
    private Cluster cluster;
    private Session session;

    private TestClientConfig config;

    public CQLGlobalState(SharedRunState sharedRunState) {
        this.sharedRunState = sharedRunState;
    }

    public Cluster getCluster() {

        if (cluster==null) {
            synchronized(this) {
                if (cluster==null) {
                    Cluster.Builder builder = Cluster.builder()
                            .addContactPoint(config.host)
                            .withPort(config.port)
                            .withCompression(ProtocolOptions.Compression.NONE);

                    if (config.user != null && !config.user.isEmpty()) {
                        builder.withCredentials(config.user, config.password);
                    }

                    cluster = builder.build();

                    sharedRunState.getMetricReporters().addRegistry(cluster.getMetrics().getRegistry());
                }
            }
        }

        System.out.println("cluster-metadata-allhosts:\n" + session.getCluster().getMetadata().getAllHosts());

        return cluster;
    }

    public Session getSession() {
        if (session == null )
            synchronized (this) {
                if(session==null) {
                    session = getCluster().newSession();
                }
            }
        return session;

    }

    // TODO: put this somewhere:         cluster.close();

}
