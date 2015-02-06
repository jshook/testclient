package com.metawiring.load.generator;

public interface GeneratorFactory<T> {
    Generator<T> getGenerator();
}
