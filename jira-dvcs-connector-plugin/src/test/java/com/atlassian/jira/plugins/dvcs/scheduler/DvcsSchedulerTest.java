package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.sync.SyncConfig;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
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
    @Mock private MessagingService mockMessagingService;
    @Mock private Synchronizer synchronizer;
    @Mock private SchedulerLauncher schedulerLauncher;
    @Mock private SyncConfig syncConfig;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(syncConfig.scheduledSyncIntervalMillis()).thenReturn(Duration.standardHours(1).getMillis());
        dvcsScheduler = new DvcsScheduler(mockMessagingService, mockScheduler, mockDvcsSchedulerJob, schedulerLauncher, syncConfig);
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
    public void onStartShouldScheduleTheJobIfItDoesNotAlreadyExist() throws Exception
    {
        // Set up
        when(mockScheduler.getJobInfo(JOB_ID)).thenReturn(null);

        // Invoke
        invokeStartupMethodsRequiredForScheduling();

        // Check
        // also testing happy path of scheduleJob
        verify(mockScheduler).registerJobHandler(JOB_HANDLER_KEY, mockDvcsSchedulerJob);
        verify(mockScheduler).getJobInfo(JOB_ID);
        verify(mockScheduler).scheduleClusteredJob(eq(JOB_ID), eq(JOB_HANDLER_KEY), any(Date.class), anyLong());
        verifyNoMoreInteractions(mockScheduler);
    }

    @Test
    public void postConstructRegisterLauncherJob() throws Exception
    {
        // Invoke
        dvcsScheduler.postConstruct();

        // Check
        verify(schedulerLauncher).runWhenReady(any(SchedulerLauncher.SchedulerLauncherJob.class));
    }

    private void invokeStartupMethodsRequiredForScheduling()
    {
        dvcsScheduler.onStart();
    }

    @Test
    public void shouldNotScheduleTheJobIfItAlreadyExists() throws Exception
    {
        // Set up
        final JobInfo mockExistingJob = mock(JobInfo.class);
        when(mockScheduler.getJobInfo(JOB_ID)).thenReturn(mockExistingJob);

        // Invoke
        dvcsScheduler.scheduleJob();

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
