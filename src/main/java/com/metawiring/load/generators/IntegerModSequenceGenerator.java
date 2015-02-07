package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.InvalidParameterException;

public class IntegerModSequenceGenerator implements FastForwardableGenerator<Integer> {
    private final static Logger logger = LoggerFactory.getLogger(IntegerModSequenceGenerator.class);

    private final int modulo;
//    AtomicInteger seq=new AtomicInteger(0);
    int seq=0;

    public IntegerModSequenceGenerator(int modulo) {
        this.modulo=modulo;
    }
    public IntegerModSequenceGenerator(String modulo) {
        this(Integer.valueOf(modulo));
    }

    @Override
    public Integer get() {
        seq++;
        int ret = seq % modulo;
        return ret;
//        int s = seq.incrementAndGet();
//        //logger.info("mod s:" + s);
//        return (s % modulo);
    }

    @Override
    public void fastForward(long fastForwardTo) {
        if (fastForwardTo>Integer.MAX_VALUE) {
            throw new InvalidParameterException("Unable to fast forward an int sequence generator with value " + fastForwardTo);
        }
        seq = (int) fastForwardTo;
//        seq = new AtomicInteger((int)fastForwardTo);
    }
}