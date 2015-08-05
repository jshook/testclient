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
package com.metawiring.load.core;

/**
 * Provides general runtime state about the currently running test execution.
 */
public class TestAgentState {

    private AgentLifeCycle agentLifeCycle = AgentLifeCycle.Loaded;
    public AgentLifeCycle getAgentLifeCycle() {
        return agentLifeCycle;
    }

    public void setAgentLifeCycle(AgentLifeCycle agentLifeCycle) {
        this.agentLifeCycle = agentLifeCycle;
    }

}
