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
package com.metawiring.load.cycles;

import com.lmax.disruptor.EventTranslatorOneArg;
import com.lmax.disruptor.RingBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

public class CycleEventProducer {

    private final static Logger logger = LoggerFactory.getLogger(CycleEventProducer.class);

    private final long startCycle;
    private final long endCycle;
    private long currentCycle;
    private long stopAtCycle;
    private final LinkedList<Long> availableCycleBuckets = new LinkedList<>(); // to avoid locking

    private final RingBuffer<CycleEvent> ringBuffer;

    private static final EventTranslatorOneArg<CycleEvent, Long> LONG_TO_CYCLEEVENT_TRANSLATOR =
            new EventTranslatorOneArg<CycleEvent, Long>() {
                @Override
                public void translateTo(CycleEvent cycleEvent, long l, Long aLong) {
                    cycleEvent.set(aLong);
                }
            };

    public CycleEventProducer(long startCycle, long endCycle, RingBuffer<CycleEvent> ringBuffer) {
        this.startCycle = startCycle;
        this.endCycle = endCycle;
        this.currentCycle = startCycle;
        this.ringBuffer = ringBuffer;
    }

    public void addCycles(long moreCycles) {
        synchronized(availableCycleBuckets) {
            availableCycleBuckets.add(moreCycles);
            availableCycleBuckets.notify();
        }
    }

    public long advanceCycles() {

        while (currentCycle <= stopAtCycle || availableCycleBuckets.peekFirst() != null) {
            while (currentCycle <= stopAtCycle) {
                ringBuffer.publishEvent(LONG_TO_CYCLEEVENT_TRANSLATOR, currentCycle++);
            }
            if (availableCycleBuckets.peekFirst() != null) {
                synchronized (this) {
                    if (availableCycleBuckets.peekFirst() != null) {
                        long incrementAvailableBy = availableCycleBuckets.removeFirst();
                        stopAtCycle += incrementAvailableBy;
                    } else {
                        logger.debug("Would have been a race condition, but wasn't: " +
                                "You have multiple concurrent callers for advanceCycles. Is this really what you want?");
                    }
                }
            }
        }
        return currentCycle;
    }

    public long getCurrentCycle() {
        return currentCycle;
    }

    public long getAvailableCycles() {
        return availableCycleBuckets.stream().reduce(0l, (a, b) -> (a + b));
    }

}
