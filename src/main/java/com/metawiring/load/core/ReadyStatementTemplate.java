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

package com.metawiring.load.core;

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.Session;
import com.metawiring.load.config.StatementDef;
import com.metawiring.load.config.TestClientConfig;
import com.metawiring.load.generator.ScopedCachingGeneratorSource;

/**
 * Captures the mappings between a non-prepared statement and the generator settings that will be paired with it.
 * Provides convenience methods for easily preparing statements as well as assigning unbound generators defs.
 * Afterwards, can be used to instantiate a thread-specific ReadyStatement object, with shared generators where appropriate.
 */
public class ReadyStatementTemplate {
    private final StatementDef yamlStatementDef;
    private final TestClientConfig testClientConfig;
    private PreparedStatement preparedStatement;
    private ScopedCachingGeneratorSource generatorSource;

    public ReadyStatementTemplate(StatementDef yamlStatementDef, ScopedCachingGeneratorSource generatorSource, TestClientConfig testClientConfig) {
        this.yamlStatementDef = yamlStatementDef;
        this.generatorSource = generatorSource;
        this.testClientConfig = testClientConfig;
    }

    public void prepare(Session session) {
        preparedStatement = session.prepare(yamlStatementDef.getCookedStatement(testClientConfig));
    }

    /**
     * This is expected to be called within the runtime scope of the user. Specifically, any ThreadLocal or other
     * such behaviors should be honored, so wait to call this until you are in the thread that will use it, as there
     * is a thread scope for generators.
     * @return a ReadyStatement for this thread.
     */
    public ReadyStatement bindGenerators(long startCycle) {
        ReadyStatement readyStatement = new ReadyStatement(generatorSource,preparedStatement,startCycle);
        for (String bindName : yamlStatementDef.getBindNamesExcept("table", "keyspace", "cl", "rf")) {
            String genName = yamlStatementDef.bindings.get(bindName);
            readyStatement.addBinding(bindName,genName);
        }

        return readyStatement;
    }
}
