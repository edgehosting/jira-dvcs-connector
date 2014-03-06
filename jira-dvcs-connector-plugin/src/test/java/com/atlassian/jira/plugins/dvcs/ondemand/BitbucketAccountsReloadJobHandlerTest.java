package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.scheduler.compat.JobHandler;
import com.atlassian.scheduler.compat.JobInfo;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BitbucketAccountsReloadJobHandlerTest
{
    @Mock private BitbucketAccountsConfigService mockAccountsConfigService;
    private JobHandler jobHandler;

    @BeforeMethod
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        jobHandler = new BitbucketAccountsReloadJobHandler();
        ReflectionTestUtils.setField(jobHandler, "accountsConfigService", mockAccountsConfigService);
    }

    @Test
    public void runningJobShouldCauseAccountsConfigServiceToReload()
    {
        // Set up
        final JobInfo mockJobInfo = mock(JobInfo.class);

        // Invoke
        jobHandler.execute(mockJobInfo);

        // Check
        verify(mockAccountsConfigService).reload();
    }
}
