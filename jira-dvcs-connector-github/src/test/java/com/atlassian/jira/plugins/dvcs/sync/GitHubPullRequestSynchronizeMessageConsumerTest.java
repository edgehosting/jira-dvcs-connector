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
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.RepositoryCommit;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    private ArgumentCaptor<Map<String, Object>> savePullRequestCaptor;

    @Mock
    private IssueService issueService;

    @Captor
    private ArgumentCaptor<Map<String, Participant>> participantsIndexCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> saveCommitCaptor;

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

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), anyString(), anyString(), anyString(), any(RepositoryPullRequestMapping.Status.class), any(Date.class), anyString(), anyInt());
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
        mockSourceAndDestination();

        when(pullRequest.getUser()).thenReturn(null);

        testedClass.onReceive(message, payload);

        assertNull(savePullRequestCaptor.getValue().get(RepositoryPullRequestMapping.AUTHOR));
    }

    @Test
    public void testEmptyTitle()
    {
        mockSourceAndDestination();

        when(pullRequest.getTitle()).thenReturn("");

        testedClass.onReceive(message, payload);

        assertEquals(savePullRequestCaptor.getValue().get(RepositoryPullRequestMapping.NAME), "");
    }

    @Test
    public void testNullTitle()
    {
        mockSourceAndDestination();

        when(pullRequest.getTitle()).thenReturn(null);

        testedClass.onReceive(message, payload);

        assertNull(savePullRequestCaptor.getValue().get(RepositoryPullRequestMapping.NAME));
    }

    @Test
    public void testMaxTitle()
    {
        mockSourceAndDestination();

        when(pullRequest.getTitle()).thenReturn(StringUtils.leftPad("title ", 1000, "long "));

        testedClass.onReceive(message, payload);

        assertEquals(savePullRequestCaptor.getValue().get(RepositoryPullRequestMapping.NAME), StringUtils.leftPad("title ", 1000, "long ").substring(0, 255));
    }

    @Test
    public void testNoParticipants() throws IOException
    {
        mockSourceAndDestination();
        when(issueService.getComments(any(IRepositoryIdProvider.class), anyInt())).thenReturn(Collections.<Comment>emptyList());
        when(gitHubPullRequestService.getComments(any(IRepositoryIdProvider.class), anyInt())).thenReturn(Collections.<CommitComment>emptyList());

        User user = mock(User.class);
        when(user.getLogin()).thenReturn("user");
        when(pullRequest.getUser()).thenReturn(user);
        testedClass.onReceive(message, payload);

        verify(pullRequestService).updatePullRequestParticipants(anyInt(), anyInt(), participantsIndexCaptor.capture());
        assertEquals(participantsIndexCaptor.getValue().size(), 1);
        Participant participant = participantsIndexCaptor.getValue().get("user");
        assertParticipant(participant,  "user");
    }

    @Test
    public void testMaxParticipants() throws IOException
    {
        mockSourceAndDestination();

        List<Comment> comments = new ArrayList<Comment>();
        for (int i = 0; i < 100; i++)
        {
            Comment comment = mock(Comment.class);
            User user = mock(User.class);
            when(user.getLogin()).thenReturn("user" + i);
            when(comment.getUser()).thenReturn(user);

            comments.add(comment);
        }

        List<CommitComment> commitComments = new ArrayList<CommitComment>();
        for (int i = 0; i < 100; i++)
        {
            CommitComment commitComment = mock(CommitComment.class);
            User user = mock(User.class);
            when(user.getLogin()).thenReturn("cUser" + i);
            when(commitComment.getUser()).thenReturn(user);

            commitComments.add(commitComment);
        }

        when(issueService.getComments(any(IRepositoryIdProvider.class), anyInt())).thenReturn(comments);
        when(gitHubPullRequestService.getComments(any(IRepositoryIdProvider.class), anyInt())).thenReturn(commitComments);

        User user = mock(User.class);
        when(user.getLogin()).thenReturn("user");
        when(pullRequest.getUser()).thenReturn(user);
        testedClass.onReceive(message, payload);

        verify(pullRequestService).updatePullRequestParticipants(anyInt(), anyInt(), participantsIndexCaptor.capture());

        Map<String, Participant> participantsIndex = participantsIndexCaptor.getValue();
        assertEquals(participantsIndex.size(), 201);

        Participant participant = participantsIndexCaptor.getValue().get("user");
        assertParticipant(participant,  "user");

        for (int i = 0; i < 100; i++)
        {
            assertParticipant(participantsIndex.get("user" + i), "user" + i);
        }

        for (int i = 0; i < 100; i++)
        {
            assertParticipant(participantsIndex.get("cUser" + i), "cUser" + i);
        }

        assertEquals(participantsIndexCaptor.getValue().size(), 201);

    }

    private void assertParticipant(final Participant participant, final String name)
    {
        assertEquals(participant.getUsername(), name);
        assertEquals(participant.getRole(), Participant.ROLE_PARTICIPANT);
    }

    @Test
    public void testCommit() throws IOException
    {
        mockSourceAndDestination();

        RepositoryCommit repositoryCommit = mockCommit("aaa");
        when(gitHubPullRequestService.getCommits(any(IRepositoryIdProvider.class), anyInt())).thenReturn(Arrays.asList(repositoryCommit));

        testedClass.onReceive(message, payload);
        verify(repositoryPullRequestDao).saveCommit(eq(repository), saveCommitCaptor.capture());

        assertEquals(saveCommitCaptor.getValue().get(RepositoryCommitMapping.NODE), "aaa");
    }

    @Test
    public void testMaxCommits() throws IOException
    {
        mockSourceAndDestination();

        List<RepositoryCommit> repositoryCommits = new ArrayList<RepositoryCommit>();
        for (int i = 0; i < 100; i++)
        {
            RepositoryCommit repositoryCommit = mockCommit("aaa" + i);
            repositoryCommits.add(repositoryCommit);
        }
        when(gitHubPullRequestService.getCommits(any(IRepositoryIdProvider.class), anyInt())).thenReturn(repositoryCommits);

        testedClass.onReceive(message, payload);
        verify(repositoryPullRequestDao, times(100)).saveCommit(eq(repository), saveCommitCaptor.capture());

        assertEquals(saveCommitCaptor.getAllValues().size(), 100);
        int i = 0;
        for ( Map<String, Object> commitMap : saveCommitCaptor.getAllValues())
        {
            assertEquals(commitMap.get(RepositoryCommitMapping.NODE), "aaa" + i++);
        }
    }

    @Test
    public void testUpdateCommits() throws IOException
    {
        mockSourceAndDestination();

        RepositoryPullRequestMapping pullRequestMapping = mock(RepositoryPullRequestMapping.class);
        when(pullRequestMapping.getLastStatus()).thenReturn("OPEN");
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(pullRequestMapping);
        when(repositoryPullRequestDao.updatePullRequestInfo(anyInt(), anyString(), anyString(), anyString(), any(RepositoryPullRequestMapping.Status.class), any(Date.class), anyString(), anyInt()))
                .thenReturn(pullRequestMapping);
        RepositoryCommitMapping commitMapping = mock(RepositoryCommitMapping.class);
        when(commitMapping.getNode()).thenReturn("original");
        when(pullRequestMapping.getCommits()).thenReturn(new RepositoryCommitMapping[] { commitMapping });

        RepositoryCommit repositoryCommit = mockCommit("aaa");
        when(gitHubPullRequestService.getCommits(any(IRepositoryIdProvider.class), anyInt())).thenReturn(Arrays.asList(repositoryCommit));

        testedClass.onReceive(message, payload);
        verify(repositoryPullRequestDao).saveCommit(eq(repository), saveCommitCaptor.capture());
        assertEquals(saveCommitCaptor.getValue().get(RepositoryCommitMapping.NODE), "aaa");

        verify(repositoryPullRequestDao).unlinkCommit(eq(repository), eq(pullRequestMapping), eq(commitMapping));
        // TODO uncomment after BBC-763 is merged
//        verify(repositoryPullRequestDao).removeCommit(eq(commitMapping));
    }

    private RepositoryCommit mockCommit(String sha)
    {
        RepositoryCommit repositoryCommit = mock(RepositoryCommit.class);
        when(repositoryCommit.getSha()).thenReturn(sha);
        Commit commit = mock(Commit.class);
        when(commit.getSha()).thenReturn(sha);
        when(commit.getAuthor()).thenReturn(mock(CommitUser.class));
        when(repositoryCommit.getCommit()).thenReturn(commit);
        return repositoryCommit;
    }

    private PullRequestMarker mockRef(String branch)
    {
        PullRequestMarker sourceRef = mock(PullRequestMarker.class);
        when(sourceRef.getRepo()).thenReturn(mock(org.eclipse.egit.github.core.Repository.class));
        when(sourceRef.getRef()).thenReturn(branch);
        return sourceRef;
    }


    private void mockSourceAndDestination()
    {
        final PullRequestMarker source = mockRef("branch");
        when(pullRequest.getHead()).thenReturn(source);
        final PullRequestMarker destination = mockRef("master");
        when(pullRequest.getBase()).thenReturn(destination);
    }
}
