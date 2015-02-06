package com.metawiring.load.generators;

import com.metawiring.load.generator.Generator;

public class StringGenerator implements Generator<String> {

    private final String string;

    public StringGenerator(String string) {
        this.string = string.intern();
    }

    @Override
    public String get() {
        return string;
    }
}
