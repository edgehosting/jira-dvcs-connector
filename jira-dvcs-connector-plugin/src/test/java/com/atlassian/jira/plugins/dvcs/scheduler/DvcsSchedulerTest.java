package com.atlassian.jira.plugins.dvcs.scheduler;

import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.config.IntervalScheduleInfo;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.Schedule;
import com.atlassian.scheduler.status.JobDetails;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.scheduler.DvcsScheduler.JOB_ID;
import static com.atlassian.jira.plugins.dvcs.scheduler.DvcsScheduler.JOB_RUNNER_KEY;
import static com.atlassian.scheduler.config.RunMode.RUN_ONCE_PER_CLUSTER;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class DvcsSchedulerTest
{
    private DvcsScheduler dvcsScheduler;
    @Mock private DvcsSchedulerJob mockDvcsSchedulerJob;
    @Mock private MessagingService mockMessagingService;
    @Mock private SchedulerService mockSchedulerService;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        dvcsScheduler = new DvcsScheduler(mockMessagingService, mockSchedulerService, mockDvcsSchedulerJob);
    }


    @Test
    public void onStartShouldScheduleTheJobIfItDoesNotAlreadyExist() throws Exception
    {
        // Set up
        when(mockSchedulerService.getJobDetails(JOB_ID)).thenReturn(null);

        // Invoke
        dvcsScheduler.onStart();

        // Check
        verify(mockMessagingService).onStart();
        verify(mockSchedulerService).registerJobRunner(JOB_RUNNER_KEY, mockDvcsSchedulerJob);
        verify(mockSchedulerService).getJobDetails(JOB_ID);
        final ArgumentCaptor<JobConfig> jobConfigCaptor = ArgumentCaptor.forClass(JobConfig.class);
        verify(mockSchedulerService).scheduleJob(eq(JOB_ID), jobConfigCaptor.capture());
        final JobConfig jobConfig = jobConfigCaptor.getValue();
        assertThat(jobConfig.getRunMode()).isEqualTo(RUN_ONCE_PER_CLUSTER);
        final Schedule schedule = jobConfig.getSchedule();
        assertThat(schedule).isNotNull();
        final IntervalScheduleInfo intervalSchedule = schedule.getIntervalScheduleInfo();
        assertThat(intervalSchedule).isNotNull();
        assertThat(intervalSchedule.getIntervalInMillis()).isGreaterThan(0);
        verifyNoMoreInteractions(mockMessagingService, mockSchedulerService);
    }
    @Test
    public void onStartShouldNotScheduleTheJobIfItAlreadyExists() throws Exception
    {
        // Set up
        final JobDetails mockExistingJob = mock(JobDetails.class);
        when(mockSchedulerService.getJobDetails(JOB_ID)).thenReturn(mockExistingJob);

        // Invoke
        dvcsScheduler.onStart();

        // Check
        verify(mockMessagingService).onStart();
        verify(mockSchedulerService).registerJobRunner(JOB_RUNNER_KEY, mockDvcsSchedulerJob);
        verify(mockSchedulerService).getJobDetails(JOB_ID);
        verifyNoMoreInteractions(mockMessagingService, mockSchedulerService);
    }

    @Test
    public void destroyShouldUnregisterTheJobRunner() throws Exception
    {
        // Invoke
        dvcsScheduler.destroy();

        // Check
        verify(mockSchedulerService).unregisterJobRunner(JOB_RUNNER_KEY);
    }
}
