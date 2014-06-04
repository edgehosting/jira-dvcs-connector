package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static com.atlassian.jira.plugins.dvcs.event.EventLimit.BRANCH;
import static com.atlassian.jira.plugins.dvcs.event.EventLimit.COMMIT;
import static com.atlassian.jira.plugins.dvcs.sync.SyncConfig.PROPERTY_KEY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class SyncConfigTest
{
    @Mock
    ApplicationProperties applicationProperties;

    @InjectMocks
    SyncConfig syncConfig;

    @Test
    public void defaultSyncIntervalIsOneHour() throws Exception
    {
        assertThat(syncConfig.scheduledSyncIntervalMillis(), equalTo(TimeUnit.HOURS.toMillis(1)));
    }

    @Test
    public void scheduledSyncCanBeOverriddenBySystemProperty() throws Exception
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

    @Test
    public void getEffectiveLimitTakesApplicationPropertyOverridesIntoAccount() throws Exception
    {
        final int maxCommits = 1;
        when(applicationProperties.getString(COMMIT.getOverrideLimitProperty())).thenReturn(String.valueOf(maxCommits));
        when(applicationProperties.getString(BRANCH.getOverrideLimitProperty())).thenReturn("this not a number");

        assertThat(syncConfig.getEffectiveLimit(COMMIT), equalTo(maxCommits));
        assertThat(syncConfig.getEffectiveLimit(BRANCH), equalTo(BRANCH.getDefaultLimit()));
    }
}
