package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Integer devide the cycle, the other side of modulo.
 */
public class LongDivStringSequenceGenerator implements FastForwardableGenerator<String> {

    private final long divisor;
    AtomicLong seq=new AtomicLong(0);

    public LongDivStringSequenceGenerator(long divisor) {
        this.divisor=divisor;
    }
    public LongDivStringSequenceGenerator(String divisor) {
        this(Long.valueOf(divisor));
    }

    @Override
    public String get() {
        return String.valueOf((seq.incrementAndGet() / divisor));
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq = new AtomicLong(fastForwardTo);
    }
}