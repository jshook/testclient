package com.metawiring.load.generators;

import com.metawiring.load.generator.Generator;

import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class DateStampGenerator implements Generator<Date>,ThreadsafeGenerator {

    public DateStampGenerator() {}

    @Override
    public Date get() {
        return new Date();
    }

}
