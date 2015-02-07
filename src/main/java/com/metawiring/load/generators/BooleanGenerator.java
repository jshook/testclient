package com.metawiring.load.generators;

import com.metawiring.load.generator.Generator;
import com.metawiring.load.generator.ThreadsafeGenerator;

public class BooleanGenerator implements Generator<Boolean>,ThreadsafeGenerator {

    private Boolean boolValue;

    public BooleanGenerator(String boolValue) {
        this(Boolean.valueOf(boolValue));
    }
    public BooleanGenerator(Boolean boolValue) {
        this.boolValue = boolValue;
    }

    @Override
    public Boolean get() {
        return boolValue;
    }
}
