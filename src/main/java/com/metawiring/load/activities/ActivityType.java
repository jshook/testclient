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

package com.metawiring.load.activities;

import com.metawiring.load.config.ActivityDef;
import com.metawiring.load.cycler.api.ActionDispenser;
import com.metawiring.load.cycler.api.ActivityInput;
import com.metawiring.load.cycler.api.MotorDispenser;

/**
 * <p>An ActivityType is the central extension point in TestClient for new types of
 * activities. At the very minimum, it must provide its name and a way to generate actions,
 * given an activity definition.</p>
 *
 * <p>An implementation of ActivityType may take control of more of the internal wiring of activities
 * if the defaults are not suitable. This is done by implementing optional interfaces as documented
 * below. When an ActivityType instance is used to create an activity, any of the optionally implemented
 * interfaces will be used instead of the core implementations.</p>
 *
 * <p>{@link ActivityInput} - This interface allows for control of the way that input values are
 * generated. These value are provided to each cycle of each motor thread for each cycle of activity.</p>
 *
 * <p>{@link MotorDispenser} - This interface allows for control of the per-thread
 * execution harness which takes inputs and applies action to them.</p>
 *
 */
public interface ActivityType {
    /**
     * Return the short name of this activity type. The fully qualified name of an activity type is
     * this value, prefixed by the package of the implementing class.
     *
     * @return An activity type name, like "diag"
     */
    String getName();

    /**
     * Provide a way to generate instances of an action. The action should be
     *
     * @return an action factory
     */
    <T extends ActivityDef> ActionDispenser getActionDispenser(T activityDef);
}
