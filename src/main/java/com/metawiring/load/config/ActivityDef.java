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

/**
 * A definition for an activity.
 */
public class ActivityDef {
    private final String name;

    private final long startCycle,endCycle;
    private final int threads;
    private int maxAsync = 1000;
    private int interCycleDelay;

    public ActivityDef(String name, long startCycle, long endCycle, int threads, int maxAsync, int interCycleDelay) {
        this.name = name;
        this.threads = threads;
        this.maxAsync = maxAsync;
        this.startCycle = startCycle;
        this.endCycle = endCycle;
        this.interCycleDelay = interCycleDelay;
    }

    public String toString() {
        if (startCycle==1) {
            return name + ":" + endCycle + ":" + threads + ":" + maxAsync;
        } else {
            return name + ":" + startCycle + "-" + endCycle + ":" + threads + ":" + maxAsync;
        }
    }

    public String getName() {
        return name;
    }

    public long getStartCycle() {
        return startCycle;
    }

    public long getEndCycle() {
        return endCycle;
    }

    /**
     * Returns the greater of threads or maxAsync. The reason for this is that maxAsync less than threads will starve
     * threads of async grants, since the async is apportioned to threads in an activity.
     * @return maxAsync, or threads if threads is greater
     */
    public int getMaxAsync() {
        if (maxAsync < threads) return threads;
        else return maxAsync;
    }

    public int getThreads() {
        return threads;
    }

    public long getTotalCycles() {
        return endCycle-startCycle;
    }

    public int getInterCycleDelay() {
        return interCycleDelay;
    }
}
