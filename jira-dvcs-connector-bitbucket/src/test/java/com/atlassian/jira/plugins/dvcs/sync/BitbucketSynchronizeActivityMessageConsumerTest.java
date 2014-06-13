package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.gzipfilter.org.apache.commons.lang.ArrayUtils;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilder;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranch;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLinks;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestHead;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestParticipant;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.google.common.collect.Lists;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

/**
 * Test
 */
public class BitbucketSynchronizeActivityMessageConsumerTest
{
    @Mock
    private MessagingService messagingService;
    @Mock
    private BitbucketClientBuilderFactory bitbucketClientBuilderFactory;
    @Mock
    private RepositoryPullRequestDao repositoryPullRequestDao;
    @Mock
    private PullRequestService pullRequestService;
    @Mock
    private RepositoryDao repositoryDao;

    @InjectMocks
    private BitbucketSynchronizeActivityMessageConsumer testedClass;

    @Mock
    private Progress progress;

    @Mock
    private Repository repository;

    @Mock
    private BitbucketRemoteClient bitbucketRemoteClient;

    private BitbucketClientBuilder bitbucketClientBuilder;

    @Mock
    private RemoteRequestor requestor;

    @InjectMocks
    private PullRequestRemoteRestpoint pullRequestRemoteRestpoint;

    @Mock
    private BitbucketPullRequest bitbucketPullRequest;

    @Mock
    private BitbucketSynchronizeActivityMessage payload;

    @Mock
    private Message<BitbucketSynchronizeActivityMessage> message;

    @Captor
    private ArgumentCaptor<Map> savePullRequestCaptor;

    private BuilderAnswer builderAnswer;

    private static class BuilderAnswer implements Answer<Object>
    {
        private List<InvocationOnMock> invocationsOnBuilder = new ArrayList<InvocationOnMock>();

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable
        {
            Object builderMock = invocation.getMock();
            if (invocation.getMethod().getReturnType().isInstance(builderMock))
            {
                invocationsOnBuilder.add(invocation);
                return builderMock;
            } else
            {
                return Mockito.RETURNS_DEFAULTS.answer(invocation);
            }
        }

        public boolean executed(String method, Object... arguments)
        {
            for (InvocationOnMock invocationOnMock : invocationsOnBuilder)
            {
                if (invocationOnMock.getMethod().getName().equals(method) && ArrayUtils.isEquals(invocationOnMock.getArguments(), arguments))
                {
                    return true;
                }
            }

            return false;
        }

        public void reset()
        {
            invocationsOnBuilder.clear();
        }
    }

    @BeforeMethod
    private void init()
    {
        pullRequestRemoteRestpoint = null;
        testedClass = null;

        MockitoAnnotations.initMocks(this);

        when(repository.getOrgName()).thenReturn("org");
        when(repository.getSlug()).thenReturn("repo");
        when(bitbucketPullRequest.getId()).thenReturn(1L);
        when(bitbucketPullRequest.getUpdatedOn()).thenReturn(new Date());
        builderAnswer = new BuilderAnswer();
        bitbucketClientBuilder = mock(BitbucketClientBuilder.class, builderAnswer);

        when(bitbucketClientBuilder.build()).thenReturn(bitbucketRemoteClient);
        when(bitbucketRemoteClient.getPullRequestAndCommentsRemoteRestpoint()).thenReturn(pullRequestRemoteRestpoint);

        BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> activityPage = Mockito.mock(BitbucketPullRequestPage.class);

        String activityUrl = String.format("/repositories/%s/%s/pullrequests/activity?pagelen=%s&page=", repository.getOrgName(), repository.getSlug(), PullRequestRemoteRestpoint.REPO_ACTIVITY_PAGESIZE);
        String pullRequestDetailUrl = String.format("/repositories/%s/%s/pullrequests/%s", repository.getOrgName(), repository.getSlug(), bitbucketPullRequest.getId());

        when(requestor.get(Mockito.startsWith(activityUrl), anyMap(), any(ResponseCallback.class))).thenReturn(activityPage);
        when(requestor.get(eq(pullRequestDetailUrl), anyMap(), any(ResponseCallback.class))).thenReturn(bitbucketPullRequest);

        BitbucketPullRequestActivityInfo activityInfo = Mockito.mock(BitbucketPullRequestActivityInfo.class);

        when(activityPage.getValues()).thenReturn(Lists.newArrayList(activityInfo));

        BitbucketPullRequestBaseActivity activity = Mockito.mock(BitbucketPullRequestBaseActivity.class);
        when(activityInfo.getActivity()).thenReturn(activity);
        when(activity.getDate()).thenReturn(new Date());
        when(activity.getUpdatedOn()).thenReturn(new Date());

        when(activityInfo.getPullRequest()).thenReturn(bitbucketPullRequest);
        BitbucketLinks bitbucketLinks = Mockito.mock(BitbucketLinks.class);
        when(bitbucketPullRequest.getLinks()).thenReturn(bitbucketLinks);
        when(bitbucketClientBuilderFactory.forRepository(repository)).thenReturn(bitbucketClientBuilder);

        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);
        when(payload.getPageNum()).thenReturn(1);

        RepositoryPullRequestMapping pullRequestMapping = Mockito.mock(RepositoryPullRequestMapping.class);
        Date updatedOn = bitbucketPullRequest.getUpdatedOn();
        long remoteId = bitbucketPullRequest.getId();
        when(pullRequestMapping.getUpdatedOn()).thenReturn(updatedOn);
        when(pullRequestMapping.getRemoteId()).thenReturn(remoteId);
        when(repositoryPullRequestDao.savePullRequest(eq(repository), savePullRequestCaptor.capture())).thenReturn(pullRequestMapping);

        BitbucketLinks links = mockLinks();
        when(bitbucketPullRequest.getLinks()).thenReturn(links);
    }

    @Test
    public void testSourceBranchDeleted()
    {
        BitbucketPullRequestHead source = Mockito.mock(BitbucketPullRequestHead.class);
        when(source.getRepository()).thenReturn(Mockito.mock(BitbucketPullRequestRepository.class));
        when(source.getBranch()).thenReturn(null);
        when(bitbucketPullRequest.getSource()).thenReturn(source);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), anyString(), anyString(), anyString(), any(RepositoryPullRequestMapping.Status.class), any(Date.class), anyString(), anyInt());
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), any(Map.class));
    }

    @Test
    public void testSourceRepositoryDeleted()
    {
        when(bitbucketPullRequest.getSource()).thenReturn(null);

        Message<BitbucketSynchronizeActivityMessage> message = Mockito.mock(Message.class);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), anyString(), anyString(), anyString(), any(RepositoryPullRequestMapping.Status.class), any(Date.class), anyString(), anyInt());
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), any(Map.class));
    }

    @Test(expectedExceptions = BitbucketRequestException.Unauthorized_401.class)
    public void testAccessDenied()
    {
        when(requestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.Unauthorized_401());
        Message<BitbucketSynchronizeActivityMessage> message = Mockito.mock(Message.class);

        testedClass.onReceive(message, payload);
    }

    @Test(expectedExceptions = BitbucketRequestException.NotFound_404.class)
    public void testNotFound()
    {
        when(requestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.NotFound_404());
        Message<BitbucketSynchronizeActivityMessage> message = Mockito.mock(Message.class);

        testedClass.onReceive(message, payload);
    }

    @Test(expectedExceptions = BitbucketRequestException.InternalServerError_500.class)
    public void testInternalServerError()
    {
        when(requestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.InternalServerError_500());
        Message<BitbucketSynchronizeActivityMessage> message = Mockito.mock(Message.class);

        testedClass.onReceive(message, payload);
    }

    @Test
    public void testNoAuthor()
    {
        BitbucketPullRequestHead source = mockRef("branch");
        BitbucketPullRequestHead destination = mockRef("master");
        when(bitbucketPullRequest.getSource()).thenReturn(source);
        when(bitbucketPullRequest.getDestination()).thenReturn(destination);

        when(bitbucketPullRequest.getAuthor()).thenReturn(null);

        Message<BitbucketSynchronizeActivityMessage> message = Mockito.mock(Message.class);

        testedClass.onReceive(message, payload);

        assertNull(savePullRequestCaptor.getValue().get(RepositoryPullRequestMapping.AUTHOR));
    }

    @Test
    public void testNoParticipants()
    {
        BitbucketPullRequestHead source = mockRef("branch");
        BitbucketPullRequestHead destination = mockRef("master");
        when(bitbucketPullRequest.getSource()).thenReturn(source);
        when(bitbucketPullRequest.getDestination()).thenReturn(destination);

        when(bitbucketPullRequest.getParticipants()).thenReturn(Collections.<BitbucketPullRequestParticipant>emptyList());

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).createParticipant(anyInt(), anyInt(), any(Participant.class));
    }

    @Test
    public void testCacheOnlyFirstPage()
    {
        BitbucketPullRequestHead source = mockRef("branch");
        BitbucketPullRequestHead destination = mockRef("master");
        when(bitbucketPullRequest.getSource()).thenReturn(source);
        when(bitbucketPullRequest.getDestination()).thenReturn(destination);

        when(payload.getPageNum()).thenReturn(1);
        testedClass.onReceive(message, payload);
        assertTrue(builderAnswer.executed("cached"));
        builderAnswer.reset();
        when(payload.getPageNum()).thenReturn(2);
        testedClass.onReceive(message, payload);
        assertFalse(builderAnswer.executed("cached"));
    }

    private BitbucketLinks mockLinks()
    {
        BitbucketLinks bitbucketLinks = new BitbucketLinks();
        BitbucketLink htmlLink = new BitbucketLink();
        bitbucketLinks.setHtml(htmlLink);

        return bitbucketLinks;
    }

    private BitbucketPullRequestHead mockRef(String branchName)
    {
        BitbucketPullRequestHead source = Mockito.mock(BitbucketPullRequestHead.class);
        BitbucketBranch bitbucketBranch = new BitbucketBranch();
        bitbucketBranch.setName(branchName);
        when(source.getRepository()).thenReturn(Mockito.mock(BitbucketPullRequestRepository.class));
        when(source.getBranch()).thenReturn(bitbucketBranch);

        return source;
    }
}