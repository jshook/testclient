package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;
import com.metawiring.load.generator.ThreadsafeGenerator;

import java.util.concurrent.atomic.AtomicInteger;

public class IntegerSequenceGenerator implements FastForwardableGenerator<Integer>,ThreadsafeGenerator {

    AtomicInteger seq=new AtomicInteger(0);

    @Override
    public Integer get() {
        return seq.incrementAndGet();
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq.set((int)fastForwardTo);
    }
}