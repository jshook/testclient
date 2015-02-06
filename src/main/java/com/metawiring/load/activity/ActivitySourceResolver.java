package com.metawiring.load.activity;

import com.metawiring.load.config.ActivityDef;

/**
 * Looks up activity sources.
 */
public interface ActivitySourceResolver {
    public ActivityInstanceSource get(ActivityDef activityDef);
}
