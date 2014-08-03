package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.CustomPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RequestError;
import org.eclipse.egit.github.core.client.RequestException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;

/**
 * GitHub pull requests synchronization test
 */
public class GitHubPullRequestSynchronizeMessageConsumerTest
{
    @InjectMocks
    private GitHubPullRequestSynchronizeMessageConsumer testedClass;

    @Mock
    private GitHubPullRequestSynchronizeMessage payload;

    @Mock
    private Message<GitHubPullRequestSynchronizeMessage> message;

    @Mock
    private CustomPullRequestService gitHubPullRequestService;

    @Mock
    private Repository repository;

    @Mock
    private PullRequest pullRequest;

    @Mock
    private Progress progress;

    @Mock
    private GithubClientProvider gitHubClientProvider;

    @BeforeMethod
    private void init() throws IOException
    {
        MockitoAnnotations.initMocks(this);

        when(repository.getOrgName()).thenReturn("org");
        when(repository.getSlug()).thenReturn("repo");

        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);

        when(gitHubClientProvider.getPullRequestService(repository)).thenReturn(gitHubPullRequestService);
        when(gitHubPullRequestService.getPullRequest(any(IRepositoryIdProvider.class), anyInt())).thenReturn(pullRequest);
    }

    @Test
    public void testAccessDenied() throws IOException
    {
        when(gitHubPullRequestService.getPullRequest(any(IRepositoryIdProvider.class), anyInt())).thenThrow(new RequestException(new RequestError(), 401));

        try
        {
            testedClass.onReceive(message, payload);

            Assert.fail("Exception is expected");
        }
        catch (RuntimeException e)
        {
            assertEquals(((RequestException) e.getCause()).getStatus(), 401);
        }
    }

    @Test
    public void testNotFound() throws IOException
    {
        when(gitHubPullRequestService.getPullRequest(any(IRepositoryIdProvider.class), anyInt())).thenThrow(new RequestException(new RequestError(), 404));

        try
        {
            testedClass.onReceive(message, payload);

            Assert.fail("Exception is expected");
        }
        catch (RuntimeException e)
        {
            assertEquals(((RequestException) e.getCause()).getStatus(), 404);
        }
    }

    @Test
    public void testInternalServerError() throws IOException
    {
        when(gitHubPullRequestService.getPullRequest(any(IRepositoryIdProvider.class), anyInt())).thenThrow(new RequestException(new RequestError(), 500));

        try
        {
            testedClass.onReceive(message, payload);

            Assert.fail("Exception is expected");
        }
        catch (RuntimeException e)
        {
            assertEquals(((RequestException) e.getCause()).getStatus(), 500);
        }
    }
}
