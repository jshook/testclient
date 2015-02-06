package com.metawiring.load.generator;

import com.metawiring.load.generator.Generator;
import com.metawiring.load.generator.GeneratorFactory;
import com.metawiring.load.generators.ThreadNumGenerator;

public class StaticGeneratorFactory implements GeneratorFactory {
    private ThreadNumGenerator gen = new ThreadNumGenerator();
    public StaticGeneratorFactory(ThreadNumGenerator threadNumGenerator) {
        this.gen=threadNumGenerator;
    }

    @Override
    public Generator getGenerator() {
        return null;
    }
}
