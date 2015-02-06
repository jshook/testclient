package com.metawiring.load.generators;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.concurrent.atomic.AtomicLong;


public class CycleNumberGenerator implements FastForwardableGenerator<Long>,ThreadsafeGenerator {

    private AtomicLong seq = new AtomicLong(0l);
    private DateTimeFormatter formatter;

    public CycleNumberGenerator() {
    }

    @Override
    public Long get() {
        long outval = seq.incrementAndGet();
        return outval;
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq = new AtomicLong(fastForwardTo);
    }
}
