package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class EventsFeatureTest
{
    @Mock
    FeatureManager featureManager;

    @InjectMocks
    EventsFeature eventsFeature;

    @Test
    public void shouldBeEnabledWhenFeatureIsDisabled() throws Exception
    {
        when(featureManager.isEnabled(EventsFeature.FEATURE_NAME)).thenReturn(false);
        assertThat(eventsFeature.isEnabled(), equalTo(true));
    }

    @Test
    public void shouldBeDisabledWhenFeatureIsEnabled() throws Exception
    {
        when(featureManager.isEnabled(EventsFeature.FEATURE_NAME)).thenReturn(true);
        assertThat(eventsFeature.isEnabled(), equalTo(false));
    }
}
