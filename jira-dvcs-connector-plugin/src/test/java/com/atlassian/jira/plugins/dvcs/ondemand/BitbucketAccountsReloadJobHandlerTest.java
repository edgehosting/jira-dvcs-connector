package com.atlassian.jira.plugins.dvcs.ondemand;

import com.atlassian.scheduler.compat.JobInfo;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BitbucketAccountsReloadJobHandlerTest
{
    @Mock private ApplicationContext mockApplicationContext;
    @Mock private BitbucketAccountsConfigService mockAccountsConfigService;
    private BitbucketAccountsReloadJobHandler jobHandler;

    @BeforeMethod
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);
        final Map<String, BitbucketAccountsConfigService> serviceMap = singletonMap("anything", mockAccountsConfigService);
        when(mockApplicationContext.getBeansOfType(BitbucketAccountsConfigService.class)).thenReturn(serviceMap);
        jobHandler = new BitbucketAccountsReloadJobHandler();
        jobHandler.setApplicationContext(mockApplicationContext);
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
