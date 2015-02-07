package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Integer devide the cycle, the other side of modulo.
 */
public class LongDivSequenceGenerator implements FastForwardableGenerator<Long> {

    private final long divisor;
    AtomicLong seq=new AtomicLong(0);

    public LongDivSequenceGenerator(long divisor) {
        this.divisor=divisor;
    }
    public LongDivSequenceGenerator(String divisor) {
        this(Long.valueOf(divisor));
    }

    @Override
    public Long get() {
        return (seq.incrementAndGet() / divisor);
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq = new AtomicLong(fastForwardTo);
    }
}