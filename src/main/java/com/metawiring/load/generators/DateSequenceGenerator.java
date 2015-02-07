package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;
import com.metawiring.load.generator.ThreadsafeGenerator;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class DateSequenceGenerator implements FastForwardableGenerator<Date>,ThreadsafeGenerator {

    private AtomicLong seq = new AtomicLong(0l);
    private long increment = 1l;

    public DateSequenceGenerator() {}
    public DateSequenceGenerator(int increment) {
        this.increment=increment;
    }
    public DateSequenceGenerator(String increment) { this.increment = Integer.valueOf(increment); }

    @Override
    public Date get() {
        long setval = seq.addAndGet(increment);
        return new Date(setval);
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq = new AtomicLong(fastForwardTo);
    }
}
