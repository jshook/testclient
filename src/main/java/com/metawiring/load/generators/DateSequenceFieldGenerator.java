package com.metawiring.load.generators;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;


public class DateSequenceFieldGenerator implements FastForwardableGenerator<String>,ThreadsafeGenerator {

    private AtomicLong seq = new AtomicLong(0l);
    private long increment = 1l;
    private DateTimeFormatter formatter;

    public DateSequenceFieldGenerator(int increment, String format) {
        this.increment=increment;
        this.formatter = DateTimeFormat.forPattern(format);
    }
    public DateSequenceFieldGenerator(String increment, String format) {
        this(Integer.valueOf(increment), format);
    }

    @Override
    public String get() {
        long setval = seq.addAndGet(increment);
        String outval = formatter.print(setval);
        return outval;
    }

    @Override
    public void fastForward(long fastForwardTo) {
        seq = new AtomicLong(fastForwardTo);
    }
}
