package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.RequestError;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.service.IssueService;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

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

    @Captor
    private ArgumentCaptor<Map> savePullRequestCaptor;

    @Mock
    private IssueService issueService;

    @Captor
    private ArgumentCaptor<Map<String, Participant>> participantsIndexCaptor;

    @BeforeMethod
    private void init() throws IOException
    {
        MockitoAnnotations.initMocks(this);
        when(pullRequest.getId()).thenReturn(1L);
        when(pullRequest.getUpdatedAt()).thenReturn(new Date());

        when(repository.getOrgName()).thenReturn("org");
        when(repository.getSlug()).thenReturn("repo");

        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);

        when(gitHubClientProvider.getPullRequestService(repository)).thenReturn(gitHubPullRequestService);
        when(gitHubClientProvider.getIssueService(repository)).thenReturn(issueService);
        when(gitHubPullRequestService.getPullRequest(any(IRepositoryIdProvider.class), anyInt())).thenReturn(pullRequest);

        RepositoryPullRequestMapping pullRequestMapping = Mockito.mock(RepositoryPullRequestMapping.class);
        Date updatedOn = pullRequest.getUpdatedAt();
        long remoteId = pullRequest.getId();
        when(pullRequestMapping.getUpdatedOn()).thenReturn(updatedOn);
        when(pullRequestMapping.getRemoteId()).thenReturn(remoteId);
        when(pullRequestMapping.getCommits()).thenReturn(new RepositoryCommitMapping[] {});
        when(pullRequestMapping.getLastStatus()).thenReturn("OPEN");
        when(repositoryPullRequestDao.savePullRequest(eq(repository), savePullRequestCaptor.capture())).thenReturn(pullRequestMapping);
    }

    @Test
    public void testSourceBranchDeleted()
    {
        PullRequestMarker sourceRef = mock(PullRequestMarker.class);
        when(sourceRef.getRepo()).thenReturn(mock(org.eclipse.egit.github.core.Repository.class));
        when(sourceRef.getRef()).thenReturn(null);
        when(pullRequest.getHead()).thenReturn(sourceRef);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), anyMap());
    }

    @Test
    public void testSourceRepositoryDeleted()
    {
        when(pullRequest.getHead()).thenReturn(null);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), anyString(), anyString(), anyString(), any(RepositoryPullRequestMapping.Status.class), any(Date.class), anyString(), anyInt());
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), any(Map.class));
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

    @Test
    public void testNoAuthor()
    {
        final PullRequestMarker source = mockRef("branch");
        when(pullRequest.getHead()).thenReturn(source);
        final PullRequestMarker destination = mockRef("master");
        when(pullRequest.getBase()).thenReturn(destination);

        when(pullRequest.getUser()).thenReturn(null);

        testedClass.onReceive(message, payload);

        assertNull(savePullRequestCaptor.getValue().get(RepositoryPullRequestMapping.AUTHOR));
    }

    @Test
    public void testNoParticipants() throws IOException
    {
        final PullRequestMarker source = mockRef("branch");
        when(pullRequest.getHead()).thenReturn(source);
        final PullRequestMarker destination = mockRef("master");
        when(pullRequest.getBase()).thenReturn(destination);
        when(issueService.getComments(any(IRepositoryIdProvider.class), anyInt())).thenReturn(Collections.<Comment>emptyList());
        when(gitHubPullRequestService.getComments(any(IRepositoryIdProvider.class), anyInt())).thenReturn(Collections.<CommitComment>emptyList());

        User user = mock(User.class);
        when(user.getLogin()).thenReturn("user");
        when(pullRequest.getUser()).thenReturn(user);
        testedClass.onReceive(message, payload);

        verify(pullRequestService).updatePullRequestParticipants(anyInt(), anyInt(), participantsIndexCaptor.capture());
        assertEquals(participantsIndexCaptor.getValue().size(), 1);
        Participant participant = participantsIndexCaptor.getValue().get("user");
        assertEquals(participant.getUsername(), "user");
        assertEquals(participant.getRole(), Participant.ROLE_PARTICIPANT);
    }

    private PullRequestMarker mockRef(String branch)
    {
        PullRequestMarker sourceRef = mock(PullRequestMarker.class);
        when(sourceRef.getRepo()).thenReturn(mock(org.eclipse.egit.github.core.Repository.class));
        when(sourceRef.getRef()).thenReturn(branch);
        return sourceRef;
    }

}
