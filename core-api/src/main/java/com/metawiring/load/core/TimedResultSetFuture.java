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
package com.metawiring.load.core;

/**
 * TODO: placeholder for a class that is sorely needed
 * There should be a way to decorate resultset futures with additional data that the driver wants to track.
 * This would avoid having to index or hash into some other structure to get future-specific metadata.
 * An example: Suppose you want to ask the driver how long "this specific operation" took, without creating lots
 * of tracking structure around it. The ability to do this in the future would keep things fast and light, as much
 * as possible.
 */
public class TimedResultSetFuture {
}
