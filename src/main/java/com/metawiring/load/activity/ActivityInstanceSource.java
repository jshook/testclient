package com.metawiring.load.activity;

/**
 * Provides instances of Activities. This allows for customization of activity scope when needed.
 */
public interface ActivityInstanceSource {

    Activity get();

    String getName();

    String getActivityName();
}
