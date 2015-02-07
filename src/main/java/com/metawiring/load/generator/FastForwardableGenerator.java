package com.metawiring.load.generator;

import com.metawiring.load.generator.Generator;

/**
 * This interface describes generators which can be fast forwarded. RNG Generators generally can not.
 * Sequence generators generally can.
 */
public interface FastForwardableGenerator<T> extends Generator<T> {
    public void fastForward(long fastForwardTo);
}
