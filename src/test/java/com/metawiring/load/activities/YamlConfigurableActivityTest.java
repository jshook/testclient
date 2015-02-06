package com.metawiring.load.activities;

import com.metawiring.load.config.TestClientConfig;
import com.metawiring.load.config.YamlActivityDef;
import com.metawiring.load.core.ExecutionContext;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

@Test
public class YamlConfigurableActivityTest {

    @Test
    public void testYamlConfigurableActivityLoadsOnInit() {
        ExecutionContext c = mock(ExecutionContext.class);
        TestClientConfig conf = TestClientConfig.builder().build();
        when(c.getConfig()).thenReturn(conf);

        YamlConfigurableActivity activity = new YamlConfigurableActivity();
        activity.init("write-telemetry",c);
        YamlActivityDef ad = activity.getYamlActivityDef();
        assertNotNull(ad);

    }
}