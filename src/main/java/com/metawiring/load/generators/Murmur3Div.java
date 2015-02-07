package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;
import de.greenrobot.common.hash.Murmur3F;

public class Murmur3Div implements FastForwardableGenerator<Long> {
    private Murmur3F murmur3f = new Murmur3F();
    private LongDivSequenceGenerator longDivSequenceGenerator;

    public Murmur3Div(long divisor) {
        this.longDivSequenceGenerator = new LongDivSequenceGenerator(divisor);
    }
    public Murmur3Div(String divisor) {
        this(Long.valueOf(divisor));
    }

    @Override
    public void fastForward(long fastForwardTo) {
        murmur3f = new Murmur3F((int) (fastForwardTo % Integer.MAX_VALUE));
    }

    @Override
    public Long get() {
        long divided= longDivSequenceGenerator.get();
        murmur3f.update((int) (divided % Integer.MAX_VALUE));
        return murmur3f.getValue();
    }
}
