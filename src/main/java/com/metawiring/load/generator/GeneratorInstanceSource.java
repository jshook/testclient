package com.metawiring.load.generator;

public interface GeneratorInstanceSource {
    public Generator getGenerator(String generatorSpec);
}
