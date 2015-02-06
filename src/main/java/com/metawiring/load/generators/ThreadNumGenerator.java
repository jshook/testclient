package com.metawiring.load.generators;

import com.metawiring.load.core.IndexedThreadFactory;
import com.metawiring.load.generator.Generator;

public class ThreadNumGenerator implements Generator<Integer> {

    @Override
    public Integer get() {
        int threadIndex= ((IndexedThreadFactory.IndexedThread)Thread.currentThread()).getThreadIndex();
        return threadIndex;
    }
}
