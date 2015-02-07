package com.metawiring.load.generators;

import com.metawiring.load.generator.Generator;
import com.metawiring.load.generator.ThreadsafeGenerator;

import java.util.Date;

public class DateStampGenerator implements Generator<Date>,ThreadsafeGenerator {

    public DateStampGenerator() {}

    @Override
    public Date get() {
        return new Date();
    }

}
