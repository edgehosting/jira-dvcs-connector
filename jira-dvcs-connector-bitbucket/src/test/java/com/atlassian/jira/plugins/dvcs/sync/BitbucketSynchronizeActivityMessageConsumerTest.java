package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilder;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLinks;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestBaseActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestHead;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.google.common.collect.Lists;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private PullRequestRemoteRestpoint pullRequestRemoteRestpoint;

    @Mock
    private BitbucketPullRequest bitbucketPullRequest;

    private static class BuilderAnswer implements Answer<Object>
    {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable
        {
            Object builderMock = invocation.getMock();
            if (invocation.getMethod().getReturnType().isInstance(builderMock))
            {
                return builderMock;
            } else
            {
                return Mockito.RETURNS_DEFAULTS.answer(invocation);
            }
        }
    }

    @BeforeMethod
    private void init()
    {
        MockitoAnnotations.initMocks(this);

        bitbucketClientBuilder = mock(BitbucketClientBuilder.class, new BuilderAnswer());

        when(bitbucketClientBuilder.build()).thenReturn(bitbucketRemoteClient);
        when(bitbucketRemoteClient.getPullRequestAndCommentsRemoteRestpoint()).thenReturn(pullRequestRemoteRestpoint);

        BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> activityPage = Mockito.mock(BitbucketPullRequestPage.class);

        when(pullRequestRemoteRestpoint.getRepositoryActivityPage(anyInt(), anyString(), anyString(), any(Date.class))).thenReturn(activityPage);
        when(pullRequestRemoteRestpoint.getPullRequestDetail(anyString(), anyString(), anyString())).thenReturn(bitbucketPullRequest);

        BitbucketPullRequestActivityInfo activityInfo = Mockito.mock(BitbucketPullRequestActivityInfo.class);

        when(activityPage.getValues()).thenReturn(Lists.newArrayList(activityInfo));

        BitbucketPullRequestBaseActivity activity = Mockito.mock(BitbucketPullRequestBaseActivity.class);
        when(activityInfo.getActivity()).thenReturn(activity);
        when(activity.getDate()).thenReturn(new Date());

        when(activityInfo.getPullRequest()).thenReturn(bitbucketPullRequest);
        when(bitbucketPullRequest.getId()).thenReturn(0L);
        BitbucketLinks bitbucketLinks = Mockito.mock(BitbucketLinks.class);
        when(bitbucketPullRequest.getLinks()).thenReturn(bitbucketLinks);
        when(bitbucketClientBuilderFactory.forRepository(repository)).thenReturn(bitbucketClientBuilder);
    }

    @Test
    public void testSourceBranchDeleted()
    {
        BitbucketSynchronizeActivityMessage payload = Mockito.mock(BitbucketSynchronizeActivityMessage.class);
        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);
        BitbucketPullRequestHead source = Mockito.mock(BitbucketPullRequestHead.class);
        when(source.getRepository()).thenReturn(Mockito.mock(BitbucketPullRequestRepository.class));
        when(bitbucketPullRequest.getSource()).thenReturn(source);

        Message<BitbucketSynchronizeActivityMessage> message = Mockito.mock(Message.class);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), anyString(), anyString(), anyString(), any(RepositoryPullRequestMapping.Status.class), any(Date.class), anyString(), anyInt());
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), any(Map.class));
    }

    @Test
    public void testSourceRepositoryDeleted()
    {
        BitbucketSynchronizeActivityMessage payload = Mockito.mock(BitbucketSynchronizeActivityMessage.class);
        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);
        when(bitbucketPullRequest.getSource()).thenReturn(null);

        Message<BitbucketSynchronizeActivityMessage> message = Mockito.mock(Message.class);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), anyString(), anyString(), anyString(), any(RepositoryPullRequestMapping.Status.class), any(Date.class), anyString(), anyInt());
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), any(Map.class));
    }
}