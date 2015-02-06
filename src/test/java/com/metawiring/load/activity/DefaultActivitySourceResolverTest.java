package com.metawiring.load.activity;

import com.metawiring.load.config.ActivityDef;
import org.testng.annotations.Test;

import static org.testng.Assert.*;

public class DefaultActivitySourceResolverTest {

    @Test
    public void testGetResolvesYamlSource() throws Exception {
        DefaultActivitySourceResolver dasr = new DefaultActivitySourceResolver();
        ActivityDef ad = new ActivityDef("write-telemetry",0,1,1,1);
        ActivityInstanceSource source = dasr.get(ad);
        assertNotNull(source);
    }

}