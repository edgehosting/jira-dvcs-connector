package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.scheduler.compat.CompatibilityPluginScheduler;
import com.atlassian.scheduler.compat.JobInfo;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static com.atlassian.jira.plugins.dvcs.ondemand.BitbucketAccountsReloadJobSchedulerImpl.A_VERY_LONG_TIME_INDEED;
import static com.atlassian.jira.plugins.dvcs.ondemand.BitbucketAccountsReloadJobSchedulerImpl.JOB_HANDLER_KEY;
import static com.atlassian.jira.plugins.dvcs.ondemand.BitbucketAccountsReloadJobSchedulerImpl.JOB_ID;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class BitbucketAccountsReloadJobSchedulerImplTest
{
    @Mock private BitbucketAccountsConfigService mockAccountsConfigService;
    @Mock private BitbucketAccountsReloadJobHandler mockJobHandler;
    @Mock private CompatibilityPluginScheduler mockScheduler;
    private BitbucketAccountsReloadJobSchedulerImpl reloadJobScheduler;

    @BeforeMethod
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        reloadJobScheduler = new BitbucketAccountsReloadJobSchedulerImpl(mockScheduler, mockJobHandler);
    }

    @Test
    public void registeringJobHandlerShouldDelegateToScheduler()
    {
        // Invoke
        reloadJobScheduler.registerJobHandler();

        // Check
        verify(mockScheduler).registerJobHandler(JOB_HANDLER_KEY, mockJobHandler);
    }

    @Test
    public void unregisteringJobHandlerShouldDelegateToScheduler()
    {
        // Invoke
        reloadJobScheduler.unregisterJobHandler();

        // Check
        verify(mockScheduler).unregisterJobHandler(JOB_HANDLER_KEY);
    }

    @Test
    public void schedulingAlreadyScheduledJobShouldDoNothing()
    {
        // Set up
        final JobInfo mockJobDetails = mock(JobInfo.class);
        when(mockScheduler.getJobInfo(JOB_ID)).thenReturn(mockJobDetails);

        // Invoke
        reloadJobScheduler.schedule();

        // Check
        verify(mockScheduler).getJobInfo(JOB_ID);
        verifyNoMoreInteractions(mockScheduler);
    }

    @Test
    public void schedulingUnscheduledJobShouldScheduleIt() throws Exception
    {
        // Set up
        when(mockScheduler.getJobInfo(JOB_ID)).thenReturn(null);

        // Invoke
        reloadJobScheduler.schedule();

        // Check
        verify(mockScheduler).scheduleClusteredJob(
                eq(JOB_ID), eq(JOB_HANDLER_KEY), any(Date.class), eq(A_VERY_LONG_TIME_INDEED));
    }
}
