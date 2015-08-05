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

import com.codahale.metrics.*;
import com.lmax.disruptor.EventHandler;

public class CycleEventHandlerSummarizer implements EventHandler<CycleEvent> {

    private final MetricRegistry metricRegistry = new MetricRegistry();
    private final Meter cycleMeter = metricRegistry.meter("cycle-meter");

    @Override
    public void onEvent(CycleEvent cycleEvent, long l, boolean b) throws Exception {
        long cycle = cycleEvent.getCycle();
        cycleMeter.mark();
        if ((cycle % 1000000)  == 0) {
            System.out.println("cycle: " + cycle);
        }
    }

    public String getSummary() {
         return "mean rate: " + cycleMeter.getMeanRate() + "\n"
                 + "total:" + cycleMeter.getCount();
    }
}
