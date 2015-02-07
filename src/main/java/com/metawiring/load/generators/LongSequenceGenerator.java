package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;
import com.metawiring.load.generator.ThreadsafeGenerator;

import java.util.concurrent.atomic.AtomicLong;

public class LongSequenceGenerator implements FastForwardableGenerator<Long>,ThreadsafeGenerator {

    AtomicLong seq=new AtomicLong(0);

    @Override
    public Long get() {
        return seq.incrementAndGet();
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq = new AtomicLong(fastForwardTo);
    }
}