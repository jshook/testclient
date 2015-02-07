package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;

import java.util.concurrent.atomic.AtomicLong;

public class LongModSequenceGenerator implements FastForwardableGenerator<Long> {

    private final long modulo;
    AtomicLong seq=new AtomicLong(0);

    public LongModSequenceGenerator(long modulo) {
        this.modulo=modulo;
    }

    @Override
    public Long get() {
        return (seq.incrementAndGet() % modulo);
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq = new AtomicLong(fastForwardTo);
    }
}