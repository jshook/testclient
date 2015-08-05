package com.metawiring.load.cycles;

import com.codahale.metrics.Counter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import org.testng.annotations.Test;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static org.testng.Assert.*;

/*
*   Copyright 2015 jshook
*   Licensed under the Apache License, Version 2.0 (the "License");
*   you may not use this file except in compliance with the License.
*   You may obtain a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*   Unless required by applicable law or agreed to in writing, software
*   distributed under the License is distributed on an "AS IS" BASIS,
*   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/
public class CycleEventProducerTest {

    @Test(enabled = false)
    public void testCycleLMAX() {

        MetricRegistry registry = new MetricRegistry();
        Counter cycleCounter = registry.counter("cycles");

        CycleEventFactory cycleEventFactory = new CycleEventFactory();
        int cycleEventBufferSize = 8192;
        Executor executor = Executors.newCachedThreadPool();

        Disruptor<CycleEvent> disruptor = new Disruptor<>(
                CycleEvent::new, cycleEventBufferSize, executor
        );

        CycleEventHandlerSummarizer eventSummarizer = new CycleEventHandlerSummarizer();
        disruptor.handleEventsWith(eventSummarizer);
//        disruptor.handleEventsWith((cycleEvent, sequence, endOfBatch) -> System.out.println("Cycle:" + cycleEvent.getCycle()));
//        cycleEventDisruptor.handleEventsWith(new CycleConsumer());
        disruptor.start();

        RingBuffer<CycleEvent> ringBuffer = disruptor.getRingBuffer();

        CycleEventProducer cep = new CycleEventProducer(1, 100000000, ringBuffer);
        cep.addCycles(100000000);
        cep.advanceCycles();
//        for (long val = 0; val < 1000000; val++) {
//            cep.advanceCycles();
//        }

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }
        System.out.println(eventSummarizer.getSummary());
    }

}