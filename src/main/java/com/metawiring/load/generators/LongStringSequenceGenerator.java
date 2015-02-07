package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;
import com.metawiring.load.generator.ThreadsafeGenerator;

import java.util.concurrent.atomic.AtomicLong;

public class LongStringSequenceGenerator implements FastForwardableGenerator<String>,ThreadsafeGenerator {

    AtomicLong seq=new AtomicLong(0);

    @Override
    public String get() {
        return String.valueOf(seq.incrementAndGet());
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq = new AtomicLong(fastForwardTo);
    }
}