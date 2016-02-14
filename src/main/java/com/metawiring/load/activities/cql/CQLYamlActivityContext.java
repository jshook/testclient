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

package com.metawiring.load.activities.cql;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.Session;
import com.metawiring.load.activities.oldcql.ActivityContext;
import com.metawiring.load.activities.oldcql.BaseActivityContext;
import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.config.YamlActivityDef;
import com.metawiring.load.core.*;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * This is the runtime shared object between all instances of a given YamlConfigurableActivity. For now, it is CQL-flavored.
 */
public class CQLYamlActivityContext extends BaseActivityContext implements ActivityContext {
    private final static Logger logger = LoggerFactory.getLogger(CQLYamlActivityContext.class);

    Timer timerOps;
    Timer timerWaits;
    Counter activityAsyncPendingCounter;
    Histogram triesHistogram;
    ReadyStatementsTemplate readyStatementsTemplate;
    Session session;
    OldExecutionContext executionContext;
    YamlActivityDef yamlActivityDef;

    public CQLYamlActivityContext(
            ActivityDef def,
            YamlActivityDef yamlActivityDef,
            ScopedCachingGeneratorSource scopedCachingGeneratorSource,
            OldExecutionContext executionContext) {
        super(def, scopedCachingGeneratorSource);

        timerOps = MetricsContext.metrics().timer(name(def.getAlias(), "ops-total"));
        timerWaits = MetricsContext.metrics().timer(name(def.getAlias(), "ops-wait"));
        activityAsyncPendingCounter = MetricsContext.metrics().counter(name(def.getAlias(), "async-pending"));
        triesHistogram = MetricsContext.metrics().histogram(name(def.getAlias(), "tries-histogram"));
        MetricsContext.metrics().meter(name(def.getAlias(), "exceptions", "PlaceHolderException"));
        session = executionContext.getSession();
        this.executionContext = executionContext;
        this.yamlActivityDef =  yamlActivityDef;
    }

    private ReadyStatementsTemplate prepareReadyStatementsTemplate() {

        Session session = executionContext.getSession();

        ReadyStatementsTemplate readyStatementsTemplate = new ReadyStatementsTemplate(
                executionContext.getSession(),
                getActivityGeneratorSource(),
                getActivityDef().getParams()
        );
        readyStatementsTemplate.addStatementsFromYaml(yamlActivityDef,"dml");
        readyStatementsTemplate.prepareAll();
        return readyStatementsTemplate;
    }

    public ReadyStatementsTemplate getReadyStatementsTemplate() {

        if (readyStatementsTemplate==null) {
            synchronized (this) {
                if (readyStatementsTemplate==null) {
                    readyStatementsTemplate = this.readyStatementsTemplate = prepareReadyStatementsTemplate();
                }
            }
        }

        return readyStatementsTemplate;
    }

    public void createSchema() {
        ReadyStatementsTemplate readyStatementsTemplate = new ReadyStatementsTemplate(
                executionContext.getSession(),
                getActivityGeneratorSource(),
                getActivityDef().getParams()
        );
        readyStatementsTemplate.addStatementsFromYaml(yamlActivityDef, "ddl");
        readyStatementsTemplate.prepareAll();
        ReadyStatements rs = readyStatementsTemplate.bindAllGenerators(0);
        for (ReadyStatement readyStatement : rs.getReadyStatements()) {
            BoundStatement bound = readyStatement.bind();
            session.execute(bound);
        }

    }
}
