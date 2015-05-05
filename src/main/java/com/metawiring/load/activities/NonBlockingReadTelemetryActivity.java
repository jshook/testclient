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

package com.metawiring.load.activities;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Timer;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Session;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.metawiring.load.activity.TimedResultSetFuture;
import com.metawiring.load.generator.GeneratorBindings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import static com.codahale.metrics.MetricRegistry.name;


/**
 * This should be written in more of a pipeline way, with consumer and producer pools, but not now.
 */
public class NonBlockingReadTelemetryActivity extends BaseActivity {

    private static Logger logger = LoggerFactory.getLogger(NonBlockingReadTelemetryActivity.class);
    private GeneratorBindings generatorBindings;

    private static final String SOURCE_FIELD = "source";
    private static final String PARAM_FIELD = "param";
    private static final String EPOCH_HOUR_FIELD = "epoch_hour";
    private static final String TIMESTAMP_FIELD = "ts";
    private static final String DATA_FIELD = "data";

    private static final String selectStmt = "select * from KEYSPACE.TABLE"
            + " where " + SOURCE_FIELD + "=? and " + EPOCH_HOUR_FIELD + "=? and " + PARAM_FIELD + "=? limit 10";

    private long endCycle, submittedCycle;
    private AtomicInteger pendingRq = new AtomicInteger();

    private long maxAsync = 0l;


    private LinkedList<TimedResultSetFuture> timedResultSetFutures = new LinkedList<>();

    private static PreparedStatement selectTelemetryStmt;
    private static Session session;

    private Timer readerTimerOps;
    private Timer readerTimerWaits;
    private Counter activityAsyncPendingCounter;
    private Histogram opsHistogram;
    private Histogram triesHistogram;

    int triesLimit = 10;

    @Override
    public void createSchema() {

    }

    @Override
    public void prepare(long startCycle, long endCycle, long maxAsync) {
        this.maxAsync = maxAsync;
        this.endCycle = endCycle;
        submittedCycle = startCycle - 1l;

        readerTimerOps = context.getMetrics().timer(name(NonBlockingReadTelemetryActivity.class.getSimpleName(), "ops-total"));
        readerTimerWaits = context.getMetrics().timer(name(NonBlockingReadTelemetryActivity.class.getSimpleName(), "ops-wait"));
        activityAsyncPendingCounter = context.getMetrics().counter(name(NonBlockingReadTelemetryActivity.class.getSimpleName(), "async-pending"));

        opsHistogram = context.getMetrics().histogram(name(NonBlockingReadTelemetryActivity.class.getSimpleName(), "ops-histogram"));
        triesHistogram = context.getMetrics().histogram(name(NonBlockingReadTelemetryActivity.class.getSimpleName(), "tries-histogram"));

        // To populate the namespace
        context.getMetrics().counter(name(getClass().getSimpleName(), "exceptions", "PlaceHolderException"));

        // This, along with static scope of the prepared stmt, will avoid "preparing the same query more than once"
        if (selectTelemetryStmt == null) {
            synchronized (NonBlockingReadTelemetryActivity.class) {
                if (selectTelemetryStmt == null) {
                    try {
                        if (selectTelemetryStmt == null) {
                            session = context.getSession();

                            if (context.getConfig().createSchema) {
                                return;
                            }

                            String statement = selectStmt;
                            statement = statement.replaceAll("KEYSPACE", context.getConfig().getKeyspace());
                            statement = statement.replaceAll("TABLE", context.getConfig().getTable());
                            selectTelemetryStmt = session.prepare(statement).setConsistencyLevel(context.getConfig().defaultConsistencyLevel);
                        }
                    } catch (Exception e) {
                        instrumentException(e);
                        throw new RuntimeException(e);
                    }
                }

            }
        }

        // The names are not used as actual parameters for now, but hopefully will later
        // At least they are useful for diagnosing generator behavior
        generatorBindings = createGeneratorBindings();
        generatorBindings.bindGenerator(selectTelemetryStmt, SOURCE_FIELD, "threadnum");
        generatorBindings.bindGenerator(selectTelemetryStmt, EPOCH_HOUR_FIELD, "date-epoch-hour", startCycle);
        generatorBindings.bindGenerator(selectTelemetryStmt, PARAM_FIELD, "varnames");
    }

    /**
     * Normalize receiving rate to 1/iterate() for now, with bias on priming rq queue
     */
    @Override
    public void iterate() {

        List<TimedResultSetFuture> timedResultSetFuturesList = Lists.newArrayListWithExpectedSize((int) maxAsync);

        // Not at limit, let the good times roll
        // This section fills the async pipeline to the configured limit
        while ((submittedCycle < endCycle) && (pendingRq.get() < maxAsync)) {
            try {

                TimedResultSetFuture trsf = new TimedResultSetFuture();
                trsf.boundStatement = selectTelemetryStmt.bind(generatorBindings.getAll());
                trsf.timerContext = readerTimerOps.time();
                trsf.rsFuture = session.executeAsync(trsf.boundStatement);
                trsf.tries++;

                timedResultSetFuturesList.add(trsf);

                activityAsyncPendingCounter.inc();
                pendingRq.incrementAndGet();
                submittedCycle++;

            } catch (Exception e) {
                String exceptionType = e.getClass().getSimpleName();
                context.getMetrics().counter(name(NonBlockingReadTelemetryActivity.class, "exceptions", exceptionType)).inc();
            }
        }

        ImmutableList<ListenableFuture<TimedResultSetFuture>> listenableFutures = Futures.inCompletionOrder(timedResultSetFuturesList);

        for (ListenableFuture<TimedResultSetFuture> future : listenableFutures) {
            try {
                Timer.Context waitTimer = null;
                waitTimer = readerTimerWaits.time();
                TimedResultSetFuture trsf = future.get();

                if (waitTimer != null) {
                    waitTimer.stop();
                }

                activityAsyncPendingCounter.dec();
                pendingRq.decrementAndGet();

                long duration = trsf.timerContext.stop();
                opsHistogram.update(duration);
                triesHistogram.update(trsf.tries);

            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }

        }

    }
}
