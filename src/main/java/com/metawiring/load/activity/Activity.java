package com.metawiring.load.activity;

import com.metawiring.load.core.ExecutionContext;

/**
 * An activity represents a single type of operation. It may be iterated many times, perhaps even separately
 * in different threads. (non-shared)
 */
public interface Activity {

    /**
     * Initialize the activity with the name and context.
     * @param name
     * @param context
     */
    void init(String name, ExecutionContext context);

    /**
     * Prepare the activity to do work.
     * The cycles are an (open,closed] form representation of the cycles which
     * this activity will be expected to run. Activities don't have to care about
     * these unless they need to know ahead of time, as in async ops where the {@link #iterate()}
     * method represents completions of operations.
     * @param startingCycle - the cycle number to start at
     * @param endingCycle - the cycle number to end at
     * @param maxAsync - total number of async operations this activity can have pending
     */
    void prepare(long startingCycle, long endingCycle, long maxAsync);

    /**
     * Create the schema for this activity. This should always use "if not exists" if possible.
     */
    void createSchema();

    /**
     * Called endingCycle - startingCycle  times after the {@link #prepare(long,long,long)} method
     */
    void iterate();

    /**
     * Called once after all calls to {@link #iterate()}
     */
    void cleanup();
}
