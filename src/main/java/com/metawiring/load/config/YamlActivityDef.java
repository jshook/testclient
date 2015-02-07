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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Just a simple configuration object for yaml activity definitions.
 */
public class YamlActivityDef {

    private List<StatementDef> ddl = new ArrayList<StatementDef>();
    private List<StatementDef> dml = new ArrayList<StatementDef>();

    public void setDdl(List<StatementDef> ddl) {
        this.ddl = ddl;
    }
    public void setDml(List<StatementDef> dml) {
        this.dml = dml;
    }

    public List<StatementDef> getDml() {
        return dml;
    }

    public List<StatementDef> getDdl() {
        return ddl;
    }

    public static class StatementDef {
        public String name="";
        public String cql="";
        public Map<String,String> bindings = new HashMap<String,String>();
        private String[] bindNames;

        Pattern bindableRegex = Pattern.compile("<<(\\w+)>>");
        /**
         * @return bindableNames in order as specified in the parameter placeholders
         */
        public List<String> getBindNames() {
            Matcher m = bindableRegex.matcher(cql);
            List<String> bindNames = new ArrayList<>();
            while (m.find()) {
                bindNames.add(m.group(1));
            }
            return bindNames;
        }

        /**
         * @return CQL statement with '?' in place of the bindable parameters, suitable for preparing
         */
        public String getCookedStatement(TestClientConfig config) {
            String statement = cql;
            statement = statement.replaceAll("<<KEYSPACE>>", config.getKeyspace());
            statement = statement.replaceAll("<<TABLE>>", config.getTable());
            statement = statement.replaceAll("<<RF>>", String.valueOf(config.defaultReplicationFactor));
            statement = statement.replaceAll("<<\\w+>>","?");
            return statement;
        }
    }



}
