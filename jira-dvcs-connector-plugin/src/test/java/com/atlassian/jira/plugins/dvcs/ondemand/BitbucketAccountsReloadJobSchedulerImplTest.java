package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.scheduler.SchedulerService;
import com.atlassian.scheduler.SchedulerServiceException;
import com.atlassian.scheduler.config.JobConfig;
import com.atlassian.scheduler.config.RunMode;
import com.atlassian.scheduler.config.Schedule;
import com.atlassian.scheduler.status.JobDetails;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.ondemand.BitbucketAccountsReloadJobSchedulerImpl.JOB_ID;
import static com.atlassian.jira.plugins.dvcs.ondemand.BitbucketAccountsReloadJobSchedulerImpl.JOB_RUNNER_KEY;
import static com.atlassian.scheduler.config.RunMode.RUN_ONCE_PER_CLUSTER;
import static com.atlassian.scheduler.config.Schedule.Type.INTERVAL;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BitbucketAccountsReloadJobSchedulerImplTest
{
    @Mock private BitbucketAccountsConfigService mockAccountsConfigService;
    @Mock private BitbucketAccountsReloadJobRunner mockJobRunner;
    @Mock private SchedulerService mockSchedulerService;
    private BitbucketAccountsReloadJobSchedulerImpl reloadJobScheduler;

    @BeforeMethod
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        reloadJobScheduler = new BitbucketAccountsReloadJobSchedulerImpl(mockSchedulerService, mockJobRunner);
    }

    @Test
    public void registeringJobRunnerShouldDelegateToSchedulerService()
    {
        // Invoke
        reloadJobScheduler.registerJobRunner();

        // Check
        verify(mockSchedulerService).registerJobRunner(JOB_RUNNER_KEY, mockJobRunner);
    }

    @Test
    public void unregisteringJobRunnerShouldDelegateToSchedulerService()
    {
        // Invoke
        reloadJobScheduler.unregisterJobRunner();

        // Check
        verify(mockSchedulerService).unregisterJobRunner(JOB_RUNNER_KEY);
    }

    @Test
    public void schedulingAlreadyScheduledJobShouldDoNothing()
    {
        // Set up
        final JobDetails mockJobDetails = mock(JobDetails.class);
        when(mockSchedulerService.getJobDetails(JOB_ID)).thenReturn(mockJobDetails);

        // Invoke
        reloadJobScheduler.schedule();

        // Check
        verify(mockSchedulerService).getJobDetails(JOB_ID);
        verifyNoMoreInteractions(mockSchedulerService);
    }

    @Test
    public void schedulingUnscheduledJobShouldScheduleIt() throws Exception
    {
        // Set up
        when(mockSchedulerService.getJobDetails(JOB_ID)).thenReturn(null);

        // Invoke
        reloadJobScheduler.schedule();

        // Check
        final ArgumentCaptor<JobConfig> jobConfigCaptor = ArgumentCaptor.forClass(JobConfig.class);
        verify(mockSchedulerService).scheduleJob(eq(JOB_ID), jobConfigCaptor.capture());
        final JobConfig jobConfig = jobConfigCaptor.getValue();
        assertThat(jobConfig.getJobRunnerKey()).isEqualTo(JOB_RUNNER_KEY);
        assertThat(jobConfig.getRunMode()).isEqualTo(RUN_ONCE_PER_CLUSTER);
        // Interval of zero means run once
        assertThat(jobConfig.getSchedule().getType()).isEqualTo(INTERVAL);
        assertThat(jobConfig.getSchedule().getIntervalScheduleInfo().getIntervalInMillis()).isEqualTo(0);
    }
}
