package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static com.atlassian.jira.plugins.dvcs.sync.SyncConfig.PROPERTY_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

@Listeners (MockitoTestNgListener.class)
public class SyncConfigTest
{
    @InjectMocks
    SyncConfig syncConfig;

    @Test
    public void defaultSyncIntervalIsOneHour() throws Exception
    {
        assertThat(syncConfig.scheduledSyncIntervalMillis(), equalTo(TimeUnit.HOURS.toMillis(1)));
    }

    @Test
    public void systemPropertyShouldOverrideDefaultSyncInterval() throws Exception
    {
        final long override = TimeUnit.MINUTES.toMillis(30);
        final String previous = System.setProperty(PROPERTY_KEY, String.valueOf(override));
        try
        {
            assertThat(syncConfig.scheduledSyncIntervalMillis(), equalTo(override));
        }
        finally
        {
            if (previous == null) { System.clearProperty(PROPERTY_KEY); }
            else { System.setProperty(PROPERTY_KEY, previous); }
        }
    }
}
