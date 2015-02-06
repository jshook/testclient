package com.metawiring.load.generators;

import com.metawiring.load.generator.Generator;
import org.apache.commons.math3.random.MersenneTwister;

import java.nio.ByteBuffer;

public class RandomBytesGenerator implements Generator<ByteBuffer> {

    private MersenneTwister twister = new MersenneTwister();
    private int length;

    public RandomBytesGenerator(int length) {
        this.length = length;
    }

    @Override
    public ByteBuffer get() {
        byte[] buffer = new byte[length];
        twister.nextBytes(buffer);
        return ByteBuffer.wrap(buffer);
    }
}
