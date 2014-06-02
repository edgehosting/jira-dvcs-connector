package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.sync.SyncConfig;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.event.events.PluginEnabledEvent;
import com.atlassian.scheduler.compat.CompatibilityPluginScheduler;
import com.atlassian.scheduler.compat.JobInfo;
import org.joda.time.Duration;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static com.atlassian.jira.plugins.dvcs.scheduler.DvcsScheduler.JOB_HANDLER_KEY;
import static com.atlassian.jira.plugins.dvcs.scheduler.DvcsScheduler.JOB_ID;
import static com.atlassian.jira.plugins.dvcs.util.DvcsConstants.PLUGIN_KEY;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DvcsSchedulerTest
{
    private DvcsScheduler dvcsScheduler;
    @Mock private CompatibilityPluginScheduler mockScheduler;
    @Mock private DvcsSchedulerJob mockDvcsSchedulerJob;
    @Mock private EventPublisher mockEventPublisher;
    @Mock private MessagingService mockMessagingService;
    @Mock private Plugin mockPlugin;
    @Mock private PluginEnabledEvent mockPluginEnabledEvent;
    @Mock private Synchronizer synchronizer;
    @Mock private SyncConfig syncConfig;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(mockPluginEnabledEvent.getPlugin()).thenReturn(mockPlugin);
        when(mockPlugin.getKey()).thenReturn(PLUGIN_KEY);
        when(syncConfig.scheduledSyncIntervalMillis()).thenReturn(Duration.standardHours(1).getMillis());
        dvcsScheduler = new DvcsScheduler(mockMessagingService, mockScheduler, mockDvcsSchedulerJob, mockEventPublisher, syncConfig);
    }

    @Test
    public void startingTheDvcsSchedulerShouldAlsoStartTheMessagingService()
    {
        // Invoke
        invokeStartupMethodsRequiredForScheduling();

        // Verify
        verify(mockMessagingService).onStart();
    }

    @Test
    public void startingTheDvcsSchedulerShouldAlsoUnregisterEventPublisher()
    {
        // Invoke
        invokeStartupMethodsRequiredForScheduling();

        // Verify
        verify(mockEventPublisher).unregister(dvcsScheduler);
    }

    @Test
    public void onStartShouldScheduleTheJobIfItDoesNotAlreadyExist() throws Exception
    {
        // Set up
        when(mockScheduler.getJobInfo(JOB_ID)).thenReturn(null);

        // Invoke
        invokeStartupMethodsRequiredForScheduling();

        // Check
        verify(mockScheduler).registerJobHandler(JOB_HANDLER_KEY, mockDvcsSchedulerJob);
        verify(mockScheduler).getJobInfo(JOB_ID);
        verify(mockScheduler).scheduleClusteredJob(eq(JOB_ID), eq(JOB_HANDLER_KEY), any(Date.class), anyLong());
        verifyNoMoreInteractions(mockScheduler);
    }

    @Test
    public void partialEventsShouldNotTriggerScheduling1() throws Exception
    {
        // Invoke
        dvcsScheduler.postConstruct();
        dvcsScheduler.onStart();

        // Check
        verifyNoMoreInteractions(mockScheduler);
        verifyNoMoreInteractions(mockMessagingService);
    }

    @Test
    public void partialEventsShouldNotTriggerScheduling2() throws Exception
    {
        // Invoke
        dvcsScheduler.postConstruct();
        dvcsScheduler.onPluginEnabled(mockPluginEnabledEvent);

        // Check
        verifyNoMoreInteractions(mockScheduler);
        verifyNoMoreInteractions(mockMessagingService);
    }

    @Test
    public void partialEventsShouldNotTriggerScheduling3() throws Exception
    {
        // Invoke
        dvcsScheduler.onStart();
        dvcsScheduler.onPluginEnabled(mockPluginEnabledEvent);

        // Check
        verifyNoMoreInteractions(mockScheduler);
        verifyNoMoreInteractions(mockMessagingService);
    }

    private void invokeStartupMethodsRequiredForScheduling()
    {
        dvcsScheduler.postConstruct();
        dvcsScheduler.onStart();
        dvcsScheduler.onPluginEnabled(mockPluginEnabledEvent);
    }

    @Test
    public void onStartShouldNotScheduleTheJobIfItAlreadyExists() throws Exception
    {
        // Set up
        final JobInfo mockExistingJob = mock(JobInfo.class);
        when(mockScheduler.getJobInfo(JOB_ID)).thenReturn(mockExistingJob);

        // Invoke
        invokeStartupMethodsRequiredForScheduling();

        // Check
        verify(mockScheduler).getJobInfo(JOB_ID);
        verify(mockScheduler).registerJobHandler(JOB_HANDLER_KEY, mockDvcsSchedulerJob);
        verifyNoMoreInteractions(mockScheduler);
    }

    @Test
    public void destroyShouldUnregisterTheJobHandler() throws Exception
    {
        // Invoke
        dvcsScheduler.destroy();

        // Check
        verify(mockScheduler).unregisterJobHandler(JOB_HANDLER_KEY);
    }
}
