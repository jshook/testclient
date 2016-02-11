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

package com.metawiring.load.activities;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Session;
import com.metawiring.load.activities.cql.ActivityContext;
import com.metawiring.load.activities.cql.BaseActivityContext;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.StatementDef;
import com.metawiring.load.core.ExecutionContext;
import com.metawiring.load.core.ReadyStatement;
import com.metawiring.load.core.ReadyStatements;
import com.metawiring.load.core.ReadyStatementsTemplate;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This is the runtime shared object between all instances of a given YamlConfigurableActivity. For now, it is CQL-flavored.
 */
public class CQLActivityContext extends BaseActivityContext implements ActivityContext {
    private final static Logger logger = LoggerFactory.getLogger(CQLActivityContext.class);

    Timer timerOps;
    Timer timerWaits;
    Counter activityAsyncPendingCounter;
    Histogram triesHistogram;
    ReadyStatementsTemplate readyStatementsTemplate;
    Session session;
    ExecutionContext executionContext;

    public CQLActivityContext(ActivityDef def, ScopedCachingGeneratorSource scopedCachingGeneratorSource, ExecutionContext executionContext) {
        super(def, scopedCachingGeneratorSource, executionContext);
        timerOps = executionContext.getMetrics().timer(name(def.getName(), "ops-total"));
        timerWaits = executionContext.getMetrics().timer(name(def.getName(), "ops-wait"));
        activityAsyncPendingCounter = executionContext.getMetrics().counter(name(def.getName(), "async-pending"));
        triesHistogram = executionContext.getMetrics().histogram(name(def.getName(), "tries-histogram"));
        executionContext.getMetrics().meter(name(def.getName(), "exceptions", "PlaceHolderException"));
        session = executionContext.getSession();
        this.executionContext = executionContext;
    }

    public ReadyStatementsTemplate initReadyStatementsTemplate(List<StatementDef> statementDefs) {

        readyStatementsTemplate = new ReadyStatementsTemplate(
                executionContext.getSession(),
                getActivityGeneratorSource(),
                executionContext.getConfig()
        );

        readyStatementsTemplate.addStatementDefs(statementDefs);
        readyStatementsTemplate.prepareAll();
        return readyStatementsTemplate;

    }

    public ReadyStatementsTemplate getReadyStatementsTemplate() {

        if (readyStatementsTemplate == null) {
            throw new RuntimeException("The ready statements template must be prepared with the initReadyStatementsTemplate(StatementDef...) method");
        }

        return readyStatementsTemplate;
    }

    public void createSchema(List<StatementDef> statementDefs) {
        ReadyStatementsTemplate readyStatementsTemplate = new ReadyStatementsTemplate(
                executionContext.getSession(),
                getActivityGeneratorSource(),
                executionContext.getConfig()
        );
        readyStatementsTemplate.addStatementDefs(statementDefs);
        readyStatementsTemplate.prepareAll();
        ReadyStatements rs = readyStatementsTemplate.bindAllGenerators(0);
        for (ReadyStatement readyStatement : rs.getReadyStatements()) {
            BoundStatement bound = readyStatement.bind();
            session.execute(bound);
        }


    }
}
