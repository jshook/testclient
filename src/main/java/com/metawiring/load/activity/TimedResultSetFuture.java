package com.metawiring.load.activity;

import com.codahale.metrics.Timer;
import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.ResultSetFuture;

/**
 * This is meant to be used internally by activity implementations, not shared in any way.
 *
 * You can use it to track the status of an async operation so that it may be retried if needed.
 */
public class TimedResultSetFuture {

    public ResultSetFuture rsFuture;

    // initialize this before you send the first request for this bound statement
    public Timer.Context timerContext;

    // The bound statement, if the result is not successful, simply resend it
    public BoundStatement boundStatement;

    // Incremented for each executeAsync
    public int tries = 0;
}
