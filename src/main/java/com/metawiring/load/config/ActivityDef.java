package com.metawiring.load.config;

/**
 * A definition for an activity.
 */
public class ActivityDef {
    private final String name;

    private final long startCycle,endCycle;
    private final int threads;
    private int maxAsync = 1000;

    public ActivityDef(String name, long startCycle, long endCycle, int threads, int maxAsync) {
        this.name = name;
        this.threads = threads;
        this.maxAsync = maxAsync;
        this.startCycle = startCycle;
        this.endCycle = endCycle;
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
}
