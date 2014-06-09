package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * GitHub pull requests synchronization test
 *
 */
public class GitHubPullRequestSynchronizeMessageConsumerTest
{
    @Mock
    private GitHubPullRequestSynchronizeMessage payload;

    @Mock
    private Message<GitHubPullRequestSynchronizeMessage> message;

    @InjectMocks
    private GitHubPullRequestSynchronizeMessageConsumer testedClass;

    @Mock
    private Progress progress;

    @Mock
    private Repository repository;

    @Mock
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Mock
    private PullRequestService pullRequestService;

    @Mock
    private GithubClientProvider gitHubClientProvider;

    @Mock
    private org.eclipse.egit.github.core.service.PullRequestService gitHubPullRequestService;

    @Mock
    private PullRequest pullRequest;

    @BeforeMethod
    private void init() throws IOException
    {
        MockitoAnnotations.initMocks(this);
        when(pullRequest.getId()).thenReturn(1L);

        when(repository.getOrgName()).thenReturn("org");
        when(repository.getSlug()).thenReturn("repo");

        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);

        when(gitHubClientProvider.getPullRequestService(repository)).thenReturn(gitHubPullRequestService);
        when(gitHubPullRequestService.getPullRequest(any(IRepositoryIdProvider.class), anyInt())).thenReturn(pullRequest);
    }

    @Test
    public void testSourceBranchDeleted()
    {
        PullRequestMarker sourceRef = Mockito.mock(PullRequestMarker.class);
        when(sourceRef.getRepo()).thenReturn(Mockito.mock(org.eclipse.egit.github.core.Repository.class));
        when(sourceRef.getRef()).thenReturn(null);
        when(pullRequest.getHead()).thenReturn(sourceRef);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), anyMap());
    }
}
