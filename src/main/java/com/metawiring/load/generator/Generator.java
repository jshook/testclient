package com.metawiring.load.generator;

public interface Generator<T> {

    /**
     * Return a sample.
     * @return generated "sample" of parameterized type T
     */
    public T get();

    /**
     * For these classes, toString is the same as the specifier string that is used to map the
     * implementation and parameters. That is, you should be able take this value and put in
     * a configuration file to reproduce the exact configured generator instance
     * @return spec string
     */
    public String toString();
}
