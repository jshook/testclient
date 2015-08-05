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

import com.codahale.metrics.Timer;

/**
 * This is meant to be used internally by activity implementations. You can use it to track the status of an
 * async operation so that it may be retried if needed. This is not implemented as a template class or anything else as
 * overbearing due to the potential overhead of additional calling leyers. Instead, follow the guidelines for how
 * to use each field.
 *
 * If you are not processing futures in order of submission, it might be better to subclass
 * the type of future you are using with fields similar to those below. For that, see {@link TimedResultSetFuture}
 *
 */
public class TimedResubmittableFuture<P,F> {

    /**
     * An operation in it's most recent pre-executed form. This is generally a statement with values, a bound statement, or
     * something similar. If this object is used to submit the operation again, it should result in exactly the same operation.
     */
    public P boundStatement;

    /**
     * This timerContext should be assigned before you submit the operation for the first time. It should be closed
     * when the operation has been completely finished, whether this is a multi-try operation or it it has been given up on.
     * The semantics of the timer are how long it takes to finally complete or give up on an operation.
     */
    public Timer.Context timerContext;

    /**
     * Increment for each time the operation instance is submitted for processing. In a system which requires no
     * operation to be resubmitted, this will always measure as 1. Examining the histogram of tries can reveal
     * how cleanly a system is running at a given saturation level.
     */
    public int tries = 0;

    /**
     * When the async operation is submitted, store the result future here.
     */
    public F futureResult;

}
