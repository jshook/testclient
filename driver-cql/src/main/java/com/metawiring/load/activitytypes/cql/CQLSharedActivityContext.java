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

import com.codahale.metrics.Timer;
import com.datastax.driver.core.Session;
import com.metawiring.load.activitytypes.ActivityType;
import com.metawiring.load.activitytypes.PhaseState;
import com.metawiring.load.activitytypes.SharedActivityState;
import com.metawiring.load.activitytypes.SharedPhaseState;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.YamlActivityDef;
import com.metawiring.load.core.ReadyStatementsTemplate;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;

public class CQLSharedActivityContext extends SharedActivityState {

    private static CQLGlobalState cqlGlobalState;
    private YamlActivityDef yamlActivityDef;
    ReadyStatementsTemplate readyStatementsTemplate;

    public CQLSharedActivityContext(SharedPhaseState sharedPhaseState,ActivityType activityType, ActivityDef activityDef) {
        super(sharedPhaseState,activityType,activityDef);

        if (cqlGlobalState==null) {
            synchronized(CQLGlobalState.class) {
                if (cqlGlobalState==null) {
                    cqlGlobalState = new CQLGlobalState(sharedPhaseState.getSharedRunState());
                }
            }
        }
    }

    private ReadyStatementsTemplate prepareReadyStatementsTemplate() {

        ReadyStatementsTemplate readyStatementsTemplate = new ReadyStatementsTemplate(
                cqlGlobalState.getSession(),
                getActivityScopedCachingGeneratorSource(),
                getSharedPhaseState().getSharedRunState().getConfig()
        );

        readyStatementsTemplate.addStatements(yamlActivityDef,"dml");
        readyStatementsTemplate.prepareAll();
        return readyStatementsTemplate;
    }

    public ReadyStatementsTemplate getReadyStatementsTemplate() {

        if (readyStatementsTemplate==null) {
            synchronized (this) {
                if (readyStatementsTemplate==null) {
                    readyStatementsTemplate = prepareReadyStatementsTemplate();
                }
            }
        }

        return readyStatementsTemplate;
    }

    public Session getSession() {
        return cqlGlobalState.getSession();
    }
}
