package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.scheduler.SchedulerLauncher.SchedulerLauncherJob;
import static com.atlassian.jira.plugins.dvcs.util.DvcsConstants.PLUGIN_KEY;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class SchedulerLauncherTest
{
    private SchedulerLauncher schedulerLauncher;

    @Mock private EventPublisher mockEventPublisher;
    @Mock private PluginEnabledEvent mockPluginEnabledEvent;
    @Mock private SchedulerLauncherJob mockJob;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        mockPluginEnabledEventWithPluginKey(mockPluginEnabledEvent, PLUGIN_KEY);

        schedulerLauncher = new SchedulerLauncher(mockEventPublisher);
    }

    @Test
    public void partialEventsShouldNotTriggerJob1() throws Exception
    {
        // Invoke
        schedulerLauncher.runWhenReady(mockJob);
        schedulerLauncher.onStart();

        // Check
        verify(mockJob, never()).run();
    }

    @Test
    public void partialEventsShouldNotTriggerJob2() throws Exception
    {
        // Invoke
        schedulerLauncher.runWhenReady(mockJob);
        schedulerLauncher.onPluginEnabled(mockPluginEnabledEvent);

        // Check
        verify(mockJob, never()).run();
    }

    @Test
    public void pluginEnabledEventWithNonMatchingPluginKeyShouldNotTriggerJob() throws Exception
    {
        // Invoke
        schedulerLauncher.runWhenReady(mockJob);
        schedulerLauncher.onStart();

        PluginEnabledEvent eventWithDifferentPluginKey = mock(PluginEnabledEvent.class);
        mockPluginEnabledEventWithPluginKey(eventWithDifferentPluginKey, "some-other-key");
        schedulerLauncher.onPluginEnabled(eventWithDifferentPluginKey);

        // Check
        verify(mockJob, never()).run();

        // This should be the last event required. If this doesn't cause mockJob to run, this test is broken,
        // because it's not running above on account of a different missing event.
        schedulerLauncher.onPluginEnabled(mockPluginEnabledEvent);

        // Check
        verify(mockJob).run();
    }

    @Test
    public void allEventsShouldTriggerJob() throws Exception
    {
        // Invoke
        schedulerLauncher.runWhenReady(mockJob);
        triggerAllEvents();

        // Check
        verify(mockJob).run();
        verify(mockEventPublisher).unregister(schedulerLauncher);
    }

    @Test
    public void allEventsShouldTriggerMultipleJobs() throws Exception
    {
        // Invoke
        SchedulerLauncherJob mockJob2 = mock(SchedulerLauncherJob.class);
        SchedulerLauncherJob mockJob3 = mock(SchedulerLauncherJob.class);
        schedulerLauncher.runWhenReady(mockJob);
        schedulerLauncher.runWhenReady(mockJob2);
        schedulerLauncher.runWhenReady(mockJob3);

        triggerAllEvents();

        // Check
        verify(mockJob).run();
        verify(mockJob2).run();
        verify(mockJob3).run();
    }

    @Test
    public void jobAddedAfterAllEventsShouldRun() throws Exception
    {
        // Invoke
        triggerAllEvents();

        schedulerLauncher.runWhenReady(mockJob);

        // Check
        verify(mockJob).run();
    }

    @Test
    public void jobAddedBeforeAndAfterAllEventsShouldAllRun() throws Exception
    {
        // Invoke
        SchedulerLauncherJob mockJob2 = mock(SchedulerLauncherJob.class);
        schedulerLauncher.runWhenReady(mockJob);

        triggerAllEvents();

        schedulerLauncher.runWhenReady(mockJob2);

        // Check
        verify(mockJob).run();
        verify(mockJob2).run();
    }

    private Plugin mockPluginWithKey(final String pluginKey)
    {
        Plugin mockPlugin = mock(Plugin.class);
        when(mockPlugin.getKey()).thenReturn(pluginKey);

        return mockPlugin;
    }

    private void mockPluginEnabledEventWithPluginKey(final PluginEnabledEvent mockPluginEnabledEvent, final String pluginKey)
    {
        Plugin mockPlugin = mockPluginWithKey(pluginKey);
        when(mockPluginEnabledEvent.getPlugin()).thenReturn(mockPlugin);
    }

    private void triggerAllEvents()
    {
        schedulerLauncher.onStart();
        schedulerLauncher.onPluginEnabled(mockPluginEnabledEvent);
    }
}
