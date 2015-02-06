package com.metawiring.load.generators;

import com.metawiring.load.generator.Generator;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.apache.commons.math3.distribution.UniformIntegerDistribution;
import uk.ydubey.formatter.numtoword.NumberInWordsFormatter;

public class NamedNumberGenerator implements Generator<String> {

    private final int popsize;
    private IntegerDistribution distribution;
    private final NumberInWordsFormatter formatter = NumberInWordsFormatter.getInstance();

    public NamedNumberGenerator(int popsize) {
        this.popsize = popsize;
        distribution = new UniformIntegerDistribution(1,this.popsize+1);
    }
    public NamedNumberGenerator(String popsize) {
        this(Integer.valueOf(popsize));
    }

    @Override
    public String get() {
        int[] value = distribution.sample(1);
        String result = formatter.format(value[0]);
        return result;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ":" + popsize;
    }
}
