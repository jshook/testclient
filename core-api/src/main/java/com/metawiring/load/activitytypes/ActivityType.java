/*
 *
 *       Copyright 2015 Jonathan Shook
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

package com.metawiring.load.activitytypes;

import com.metawiring.load.activity.Activity;
import com.metawiring.load.activity.ActivityDispenserFactory;
import com.metawiring.load.activity.ActivityDispenserLocator;
import com.metawiring.load.activity.FunctionalFactory;
import com.metawiring.load.config.ActivityDef;

/**
 * The top-level binding for an activity type. It's really a hackish binding to get around type-erasure.
 */
public interface ActivityType<A extends Activity, S extends SharedActivityState> {

    /**
     * Get an activity dispenser locator that can produce an activity dispenser instance for
     * each named activity of the matching type
     */
    ActivityDispenserLocator getActivityDispenserLocator();

    /**
     * @return a dispenser factory for activitytypes of parameterized type A
     */
    ActivityDispenserFactory<A> getActivityDispenserFactory();

    S getSharedActivityState(SharedPhaseState sharedPhaseState, ActivityType at, ActivityDef ad);

//    /**
//     * What will produce the shared state for this activity type?
//     *
//     * @return a factory that produces instances of a SharedActivityState subtype for this activity type
//     */
//    FunctionalFactory<? extends SharedActivityState, ? extends SharedPhaseState> getSharedActivityStateFunction();

}
