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

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatementDef {
    private String name="";
    private String cql="";

    private Map<String,String> bindings = new HashMap<String,String>();
    private String[] bindNames;
    private Map<String,String> params = new HashMap<>();

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

    public List<String> getBindNamesExcept(String... exceptNames) {
        Set<String> exceptNamesSet = new HashSet<String>();
        Arrays.asList(exceptNames).stream().map(String::toLowerCase).forEach(exceptNamesSet::add);
        List<String> names = new ArrayList<String>();
        List<String> allBindNames = getBindNames();
        allBindNames.stream().filter(s -> !exceptNamesSet.contains(s.toLowerCase())).forEach(names::add);
        return names;
    }

    /**
     * @return CQL statement with '?' in place of the bindable parameters, suitable for preparing
     */
    public String getCookedStatement(TestClientConfig config) {
        String statement = cql;
        statement = statement.replaceAll("<<KEYSPACE>>", config.keyspace);
        statement = statement.replaceAll("<<TABLE>>", config.table);
        statement = statement.replaceAll("<<RF>>", String.valueOf(config.defaultReplicationFactor));
        statement = statement.replaceAll("<<\\w+>>","?");
        return statement;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }


    public Map<String, String> getBindings() {
        return bindings;
    }
}
