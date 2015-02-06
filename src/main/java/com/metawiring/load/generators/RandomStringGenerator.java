package com.metawiring.load.generators;

import com.metawiring.load.generator.Generator;
import org.apache.commons.math3.random.MersenneTwister;

public class RandomStringGenerator implements Generator<String> {
    private MersenneTwister theTwister = new MersenneTwister(System.nanoTime());

    @Override
    public String get() {
        return String.valueOf(Math.abs(theTwister.nextLong()));
    }
}
