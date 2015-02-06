package com.metawiring.load.activity;

import org.apache.commons.lang3.reflect.ConstructorUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DefaultActivityInstanceSource implements ActivityInstanceSource {
    private final static Logger logger = LoggerFactory.getLogger(DefaultActivityInstanceSource.class);

    private Class<? extends Activity> activityClass;
    private String activityName;

    public DefaultActivityInstanceSource(Class<? extends Activity> activityClass, String activityName) {
        this.activityClass = activityClass;
        this.activityName = activityName;
    }

    @SuppressWarnings("unchecked")
    public DefaultActivityInstanceSource(String activityClassName, String activityName) {
        try {
            this.activityClass = (Class<? extends Activity>) Class.forName(activityClassName);
            this.activityName = activityName;
        } catch (ClassNotFoundException e) {
            logger.error("Unable to find class " + activityClassName + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Activity get() {
        Activity activity;
        try {
            activity = (Activity) ConstructorUtils.invokeConstructor(activityClass, new Object[]{});
        } catch (Exception e) {
            logger.error("Error instantiating " + activityClass.getCanonicalName() + ": " + e.getMessage());
            throw new RuntimeException(e);
        }
        return activity;
    }

    @Override
    public String getName() {
        return getClass().getSimpleName() + " activityClass:" + activityClass.getCanonicalName();
    }

    @Override
    public String getActivityName() {
        return activityName;
    }

    public String toString() {
        return getClass().getCanonicalName() + " activityClass:" + activityClass.getCanonicalName();
    }
}
