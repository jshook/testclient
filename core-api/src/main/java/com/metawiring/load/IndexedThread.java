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
package com.metawiring.load;

public class IndexedThread extends Thread {

    private int threadIndex;
    private String metricName = "default-name-" + Thread.currentThread().getName();

    public IndexedThread(int threadIndex, Runnable r) {
        super(r);
        this.threadIndex = threadIndex;
    }

    public int getThreadIndex() {
        return threadIndex;
    }

    public void setMetricName(String metricName) {
        this.metricName = metricName;
    }

    public String getMetricName() {
        return metricName;
    }

}
