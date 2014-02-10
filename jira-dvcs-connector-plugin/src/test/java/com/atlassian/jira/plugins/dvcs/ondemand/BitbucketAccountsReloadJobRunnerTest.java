package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.scheduler.JobRunner;
import com.atlassian.scheduler.JobRunnerRequest;
import com.atlassian.scheduler.JobRunnerResponse;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.atlassian.scheduler.JobRunnerResponse.success;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BitbucketAccountsReloadJobRunnerTest
{
    @Mock private BitbucketAccountsConfigService mockAccountsConfigService;
    private JobRunner jobRunner;

    @BeforeMethod
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        jobRunner = new BitbucketAccountsReloadJobRunner(mockAccountsConfigService);
    }

    @Test
    public void runningJobShouldCauseAccountsConfigServiceToReload()
    {
        // Set up
        final JobRunnerRequest jobRunnerRequest = mock(JobRunnerRequest.class);

        // Invoke
        final JobRunnerResponse response = jobRunner.runJob(jobRunnerRequest);

        // Check
        verify(mockAccountsConfigService).reload();
        assertThat(response).isEqualTo(success());
    }
}
