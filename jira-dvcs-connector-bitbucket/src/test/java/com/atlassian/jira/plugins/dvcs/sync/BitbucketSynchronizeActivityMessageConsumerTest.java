package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilder;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranch;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLinks;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommitAuthor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestHead;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestParticipant;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestUpdateActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketUser;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.atlassian.jira.plugins.dvcs.util.RepositoryPullRequestMappingMock;
import com.google.common.collect.Lists;
import org.apache.commons.lang3.StringUtils;
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

import static com.atlassian.jira.plugins.dvcs.model.PullRequestStatus.DECLINED;
import static com.atlassian.jira.plugins.dvcs.model.PullRequestStatus.MERGED;
import static com.atlassian.jira.plugins.dvcs.model.PullRequestStatus.OPEN;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

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

    @Mock
    private BitbucketRemoteClient cachedBitbucketRemoteClient;

    @Mock
    private RemoteRequestor requestor;

    @Mock
    private RemoteRequestor cachedRequestor;

    @Mock
    private BitbucketPullRequest bitbucketPullRequest;

    @Mock
    private BitbucketSynchronizeActivityMessage payload;

    @Mock
    private Message<BitbucketSynchronizeActivityMessage> message;

    @Captor
    private ArgumentCaptor<RepositoryPullRequestMapping> savePullRequestCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Participant>> participantsIndexCaptor;

    @Captor
    private ArgumentCaptor<Map<String, Object>> saveCommitCaptor;

    @Mock
    private BitbucketPullRequestUpdateActivity activity;

    @Mock
    private RepositoryPullRequestMapping pullRequestMapping;

    private RepositoryPullRequestMappingMock target;
    private BitbucketPullRequest source;
    private static final String AUTHOR = "joe";
    private static final String USER = "anna";

    private class BuilderAnswer implements Answer<Object>
    {
        private boolean cached;

        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable
        {
            Object builderMock = invocation.getMock();
            if (invocation.getMethod().getReturnType().isInstance(builderMock))
            {
                if (invocation.getMethod().getName().equals("cached"))
                {
                    cached = true;
                }
                return builderMock;
            }
            else
            {
                return cached ? cachedBitbucketRemoteClient : bitbucketRemoteClient;
            }
        }
    }

    @BeforeMethod
    private void init()
    {
        testedClass = null;

        MockitoAnnotations.initMocks(this);

        when(repository.getOrgName()).thenReturn("org");
        when(repository.getSlug()).thenReturn("repo");
        when(bitbucketPullRequest.getId()).thenReturn(1L);
        when(bitbucketPullRequest.getUpdatedOn()).thenReturn(new Date());
        BitbucketPullRequestHead sourceBranch = mockRef("branch");
        BitbucketPullRequestHead destinationBranch = mockRef("master");
        when(bitbucketPullRequest.getSource()).thenReturn(sourceBranch);
        when(bitbucketPullRequest.getDestination()).thenReturn(destinationBranch);
        when(bitbucketPullRequest.getState()).thenReturn("open");

        when(bitbucketRemoteClient.getPullRequestAndCommentsRemoteRestpoint()).thenReturn(new PullRequestRemoteRestpoint(requestor));
        when(cachedBitbucketRemoteClient.getPullRequestAndCommentsRemoteRestpoint()).thenReturn(new PullRequestRemoteRestpoint(cachedRequestor));

        BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> activityPage = Mockito.mock(BitbucketPullRequestPage.class);

        String activityUrl = String.format("/repositories/%s/%s/pullrequests/activity?pagelen=%s&page=", repository.getOrgName(), repository.getSlug(), PullRequestRemoteRestpoint.REPO_ACTIVITY_PAGESIZE);
        String pullRequestDetailUrl = String.format("/repositories/%s/%s/pullrequests/%s", repository.getOrgName(), repository.getSlug(), bitbucketPullRequest.getId());

        when(requestor.get(Mockito.startsWith(activityUrl), anyMap(), any(ResponseCallback.class))).thenReturn(activityPage);
        when(requestor.get(eq(pullRequestDetailUrl), anyMap(), any(ResponseCallback.class))).thenReturn(bitbucketPullRequest);
        when(cachedRequestor.get(Mockito.startsWith(activityUrl), anyMap(), any(ResponseCallback.class))).thenReturn(activityPage);
        when(cachedRequestor.get(eq(pullRequestDetailUrl), anyMap(), any(ResponseCallback.class))).thenReturn(bitbucketPullRequest);

        when(pullRequestMapping.getLastStatus()).thenReturn("OPEN");
        when(pullRequestMapping.getUpdatedOn()).thenReturn(new Date(0L));
        when(pullRequestMapping.getCommits()).thenReturn(new RepositoryCommitMapping[] { });
        long remoteId = bitbucketPullRequest.getId();
        when(pullRequestMapping.getRemoteId()).thenReturn(remoteId);

        BitbucketPullRequestActivityInfo activityInfo = Mockito.mock(BitbucketPullRequestActivityInfo.class);

        when(activityPage.getValues()).thenReturn(Lists.newArrayList(activityInfo));

        when(activityInfo.getActivity()).thenReturn(activity);
        when(activity.getState()).thenReturn(PullRequestStatus.OPEN.name());
        Date updatedOnDate = new Date();
        when(activity.getDate()).thenReturn(updatedOnDate);
        when(activity.getUpdatedOn()).thenReturn(updatedOnDate);

        BitbucketPullRequestHead source = mock(BitbucketPullRequestHead.class);
        BitbucketPullRequestRepository sourceRepository = mock(BitbucketPullRequestRepository.class);
        when(source.getRepository()).thenReturn(sourceRepository);
        when(activity.getSource()).thenReturn(source);

        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(pullRequestMapping);
        when(repositoryPullRequestDao.updatePullRequestInfo(anyInt(), any(RepositoryPullRequestMapping.class)))
                .thenReturn(pullRequestMapping);

        final BitbucketPullRequestCommit commit = mock(BitbucketPullRequestCommit.class);
        when(commit.getHash()).thenReturn("aaa");
        BitbucketPullRequestCommitAuthor commitAuthor = mock(BitbucketPullRequestCommitAuthor.class);
        BitbucketUser user = mock(BitbucketUser.class);
        when(commitAuthor.getUser()).thenReturn(user);

        when(commit.getAuthor()).thenReturn(commitAuthor);
        BitbucketPullRequestPage<BitbucketPullRequestCommit> commitsPage = mock(BitbucketPullRequestPage.class);
        when(commitsPage.getValues()).thenReturn(Lists.newArrayList(commit));

        when(requestor.get(startsWith("commitsLink"), anyMap(), any(ResponseCallback.class))).thenReturn(commitsPage);

        when(activityInfo.getPullRequest()).thenReturn(bitbucketPullRequest);
        when(bitbucketClientBuilderFactory.forRepository(repository)).then(new Answer<BitbucketClientBuilder>()
        {
            @Override
            public BitbucketClientBuilder answer(final InvocationOnMock invocation) throws Throwable
            {
                BuilderAnswer builderAnswer = new BuilderAnswer();
                BitbucketClientBuilder bitbucketClientBuilder = mock(BitbucketClientBuilder.class, builderAnswer);
                return bitbucketClientBuilder;
            }
        });

        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);
        when(payload.getPageNum()).thenReturn(1);

        when(pullRequestService.createPullRequest(savePullRequestCaptor.capture())).thenAnswer(new Answer<RepositoryPullRequestMapping>()
        {
            @Override
            public RepositoryPullRequestMapping answer(final InvocationOnMock invocation) throws Throwable
            {
                return (RepositoryPullRequestMapping)invocation.getArguments()[0];
            }
        });

        when(pullRequestService.updatePullRequest(eq(pullRequestMapping.getID()), savePullRequestCaptor.capture())).thenAnswer(new Answer<RepositoryPullRequestMapping>()
        {
            @Override
            public RepositoryPullRequestMapping answer(final InvocationOnMock invocation) throws Throwable
            {
                return (RepositoryPullRequestMapping)invocation.getArguments()[1];
            }
        });

        BitbucketLinks links = mockLinks();
        when(bitbucketPullRequest.getLinks()).thenReturn(links);

        target = new RepositoryPullRequestMappingMock();
        when(repositoryPullRequestDao.createPullRequest()).thenReturn(target);
    }


    @Test
    public void toDaoModelPullRequest_fieldExecutedByShouldBeAuthorForPullRequestOpened()
    {
        toDaoModelPullRequest_validateFieldExecutedBy(null, OPEN, AUTHOR, AUTHOR);
    }

    @Test
    public void toDaoModelPullRequest_fieldExecutedByShouldBeUserForPullRequestMerged()
    {
        toDaoModelPullRequest_validateFieldExecutedBy(USER, MERGED, USER, AUTHOR);
    }

    @Test
    public void toDaoModelPullRequest_fieldExecutedByShouldBeUserForPullRequestDeclined()
    {
        toDaoModelPullRequest_validateFieldExecutedBy(USER, DECLINED, USER, AUTHOR);
    }

    @Test
    public void testSourceBranchDeleted()
    {
        BitbucketPullRequestHead source = Mockito.mock(BitbucketPullRequestHead.class);
        when(source.getRepository()).thenReturn(Mockito.mock(BitbucketPullRequestRepository.class));
        when(source.getBranch()).thenReturn(null);
        when(bitbucketPullRequest.getSource()).thenReturn(source);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), any(RepositoryPullRequestMapping.class));
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), any(Map.class));
    }

    @Test
    public void testSourceRepositoryDeleted()
    {
        when(bitbucketPullRequest.getSource()).thenReturn(null);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), any(RepositoryPullRequestMapping.class));
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), any(Map.class));
    }

    @Test (expectedExceptions = BitbucketRequestException.Unauthorized_401.class)
    public void testAccessDenied()
    {
        when(requestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.Unauthorized_401());
        when(cachedRequestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.Unauthorized_401());

        testedClass.onReceive(message, payload);
    }

    @Test (expectedExceptions = BitbucketRequestException.NotFound_404.class)
    public void testNotFound()
    {
        when(requestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.NotFound_404());
        when(cachedRequestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.NotFound_404());

        testedClass.onReceive(message, payload);
    }

    @Test (expectedExceptions = BitbucketRequestException.InternalServerError_500.class)
    public void testInternalServerError()
    {
        when(requestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.InternalServerError_500());
        when(cachedRequestor.get(anyString(), anyMap(), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.InternalServerError_500());

        testedClass.onReceive(message, payload);
    }

    @Test
    public void testNoAuthor()
    {
        when(bitbucketPullRequest.getAuthor()).thenReturn(null);
        // to save new value instead update
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        testedClass.onReceive(message, payload);

        assertNull(savePullRequestCaptor.getValue().getAuthor());
    }

    @Test
    public void testNullTitle()
    {
        when(bitbucketPullRequest.getTitle()).thenReturn(null);
        // to save new value instead update
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        testedClass.onReceive(message, payload);

        assertNull(savePullRequestCaptor.getValue().getName());
    }

    @Test
    public void testMaxTitle()
    {
        when(bitbucketPullRequest.getTitle()).thenReturn(StringUtils.leftPad("title ", 1000, "long "));
        // to save new value instead update
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        testedClass.onReceive(message, payload);

        assertEquals(savePullRequestCaptor.getValue().getName(), StringUtils.leftPad("title ", 1000, "long ").substring(0, 255));
    }

    @Test
    public void testNoParticipants()
    {
        when(bitbucketPullRequest.getParticipants()).thenReturn(Collections.<BitbucketPullRequestParticipant>emptyList());

        testedClass.onReceive(message, payload);

        verify(pullRequestService).updatePullRequestParticipants(anyInt(), anyInt(), participantsIndexCaptor.capture());
        assertTrue(participantsIndexCaptor.getValue().isEmpty());
    }

    @Test
    public void testMaxParticipants()
    {
        List<BitbucketPullRequestParticipant> participants = new ArrayList<BitbucketPullRequestParticipant>();
        for (int i = 0; i < 1000; i++)
        {
            BitbucketPullRequestParticipant participant = new BitbucketPullRequestParticipant();
            participant.setRole(i % 2 == 0 ? Participant.ROLE_PARTICIPANT : Participant.ROLE_REVIEWER);
            participant.setApproved(i % 4 == 0 ? true : false);
            BitbucketUser bitbucketUser = new BitbucketUser();
            bitbucketUser.setUsername("User" + i);
            participant.setUser(bitbucketUser);
            participants.add(participant);
        }

        when(bitbucketPullRequest.getParticipants()).thenReturn(participants);

        testedClass.onReceive(message, payload);

        verify(pullRequestService).updatePullRequestParticipants(anyInt(), anyInt(), participantsIndexCaptor.capture());
        Map<String, Participant> participantsIndex = participantsIndexCaptor.getValue();
        assertEquals(participantsIndex.size(), 1000);
        for (int i = 0; i < 1000; i++)
        {
            assertEquals(participantsIndex.get("User" + i).getUsername(), "User" + i);
        }
    }

    @Test
    public void testCacheOnlyFirstPage()
    {
        when(payload.getPageNum()).thenReturn(1);
        testedClass.onReceive(message, payload);
        verify(cachedRequestor, times(1)).get(anyString(), anyMap(), any(ResponseCallback.class));
    }

    @Test
    public void testNoCacheSecondPage()
    {
        when(payload.getPageNum()).thenReturn(2);
        testedClass.onReceive(message, payload);
        verify(cachedRequestor, never()).get(anyString(), anyMap(), any(ResponseCallback.class));
    }

    @Test
    public void testCommit()
    {
        final BitbucketPullRequestCommit commit = mock(BitbucketPullRequestCommit.class);
        when(commit.getHash()).thenReturn("aaa");
        BitbucketPullRequestCommitAuthor commitAuthor = mock(BitbucketPullRequestCommitAuthor.class);
        BitbucketUser user = mock(BitbucketUser.class);
        when(commitAuthor.getUser()).thenReturn(user);

        when(commit.getAuthor()).thenReturn(commitAuthor);
        BitbucketPullRequestPage<BitbucketPullRequestCommit> commitsPage = mock(BitbucketPullRequestPage.class);
        when(commitsPage.getValues()).thenReturn(Lists.newArrayList(commit));

        when(requestor.get(startsWith("commitsLink"), anyMap(), any(ResponseCallback.class))).thenReturn(commitsPage);

        testedClass.onReceive(message, payload);
        verify(repositoryPullRequestDao).saveCommit(eq(repository), saveCommitCaptor.capture());

        assertEquals(saveCommitCaptor.getValue().get(RepositoryCommitMapping.NODE), "aaa");
    }

    @Test
    public void testMaxCommits()
    {
        List<BitbucketPullRequestCommit> commits = new ArrayList<BitbucketPullRequestCommit>();
        for (int i = 0; i < 100; i++)
        {
            final BitbucketPullRequestCommit commit = mock(BitbucketPullRequestCommit.class);
            when(commit.getHash()).thenReturn("aaa" + i);
            BitbucketPullRequestCommitAuthor commitAuthor = mock(BitbucketPullRequestCommitAuthor.class);
            BitbucketUser user = mock(BitbucketUser.class);
            when(commitAuthor.getUser()).thenReturn(user);
            when(commit.getAuthor()).thenReturn(commitAuthor);

            commits.add(commit);
        }
        BitbucketPullRequestPage<BitbucketPullRequestCommit> commitsPage = mock(BitbucketPullRequestPage.class);

        when(commitsPage.getValues()).thenReturn(commits);

        when(requestor.get(startsWith("commitsLink"), anyMap(), any(ResponseCallback.class))).thenReturn(commitsPage);

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
    public void testUpdateCommit()
    {
        RepositoryCommitMapping commitMapping = mock(RepositoryCommitMapping.class);
        when(commitMapping.getNode()).thenReturn("original");
        when(pullRequestMapping.getCommits()).thenReturn(new RepositoryCommitMapping[] { commitMapping });
        target.setCommits(new RepositoryCommitMapping[] { commitMapping });

        testedClass.onReceive(message, payload);
        verify(repositoryPullRequestDao).saveCommit(eq(repository), saveCommitCaptor.capture());

        assertEquals(saveCommitCaptor.getValue().get(RepositoryCommitMapping.NODE), "aaa");

        // TODO uncomment after BBC-763 is merged
//        verify(repositoryPullRequestDao).unlinkCommit(eq(repository), eq(target), eq(commitMapping));
//        verify(repositoryPullRequestDao).removeCommit(eq(commitMapping));
    }

    private BitbucketLinks mockLinks()
    {
        BitbucketLinks bitbucketLinks = new BitbucketLinks();
        bitbucketLinks.setHtml(mockLink("htmlLink"));
        bitbucketLinks.setCommits(mockLink("commitsLink"));

        return bitbucketLinks;
    }

    private BitbucketLink mockLink(final String link)
    {
        BitbucketLink bitbucketLink = mock(BitbucketLink.class);
        when(bitbucketLink.getHref()).thenReturn(link);
        return bitbucketLink;
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

    private void setDataForExecutedByTests()
    {
        source = new BitbucketPullRequest();
        source.setAuthor(createAccount(AUTHOR));

        target = new RepositoryPullRequestMappingMock();
        when(repositoryPullRequestDao.createPullRequest()).thenReturn(target);
        when(pullRequestMapping.getSourceBranch()).thenReturn("source-branch");
        when(pullRequestMapping.getDestinationBranch()).thenReturn("dest-branch");
        when(repository.getId()).thenReturn(1);

        source.setLinks(new BitbucketLinks());
        source.getLinks().setHtml(new BitbucketLink());
        source.getLinks().getHtml().setHref("some-ref");
    }

    private void toDaoModelPullRequest_validateFieldExecutedBy(String closedBy, PullRequestStatus prStatus,
            String expectedExecutedBy, String expectedAuthor)
    {
        setDataForExecutedByTests();

        source.setClosedBy(createAccount(closedBy));
        source.setState(prStatus.name());

        RepositoryPullRequestMapping prMapping = testedClass.toDaoModelPullRequest(source, repository, pullRequestMapping, 0);

        assertEquals(expectedExecutedBy, prMapping.getExecutedBy());
        assertEquals(expectedAuthor, prMapping.getAuthor());
    }

    private BitbucketAccount createAccount(String login)
    {
        BitbucketAccount user = new BitbucketAccount();
        user.setUsername(login);
        return user;
    }

}