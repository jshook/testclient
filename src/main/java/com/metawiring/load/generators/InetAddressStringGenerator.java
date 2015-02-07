package com.metawiring.load.generators;

import com.metawiring.load.generator.FastForwardableGenerator;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

public class InetAddressStringGenerator implements FastForwardableGenerator<String> {
    private AtomicInteger atomicInt = new AtomicInteger();

    @Override
    public String get() {
        int image = atomicInt.incrementAndGet();
        ByteBuffer bytes = ByteBuffer.allocate(4);
        bytes.clear();
        bytes.putInt(image);
        bytes.flip();
        try {
            return Inet4Address.getByAddress(bytes.array()).getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public void fastForward(long fastForwardTo) {
        atomicInt.set((int)fastForwardTo % Integer.MAX_VALUE);
    }
}
