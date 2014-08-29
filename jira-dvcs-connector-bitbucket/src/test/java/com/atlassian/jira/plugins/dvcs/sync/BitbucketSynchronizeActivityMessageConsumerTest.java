package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.event.IssuesChangedEvent;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.NotificationService;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilder;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranch;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLink;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketLinks;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPageIterator;
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import org.hamcrest.FeatureMatcher;
import org.hamcrest.Matchers;
import org.hamcrest.collection.IsMapContaining;
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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsIterableContainingInAnyOrder.containsInAnyOrder;
import static org.mockito.AdditionalAnswers.returnsFirstArg;
import static org.mockito.AdditionalAnswers.returnsSecondArg;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.startsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

public class BitbucketSynchronizeActivityMessageConsumerTest
{
    private static final String COMMIT_NODE_MERGE = "merge-commit";
    private static final String COMMIT_NODE_NON_MERGE = "non-merge-commit";
    private static final String COMMIT_NODE = "aaa";
    private static final String COMMIT_NODE_2 = "bbb";
    private static final String COMMIT_NODE_ORIGINAL = "original";

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

    @Mock
    private SyncDisabledHelper syncDisabledHelper;

    @Mock
    private FeatureManager featureManager;

    @Mock
    private NotificationService notificationService;

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

        when(requestor.get(Mockito.startsWith(activityUrl), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenReturn(activityPage);
        when(requestor.get(eq(pullRequestDetailUrl), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenReturn(bitbucketPullRequest);
        when(cachedRequestor.get(Mockito.startsWith(activityUrl), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenReturn(activityPage);
        when(cachedRequestor.get(eq(pullRequestDetailUrl), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenReturn(bitbucketPullRequest);

        when(pullRequestMapping.getLastStatus()).thenReturn("OPEN");
        when(pullRequestMapping.getUpdatedOn()).thenReturn(new Date(0L));
        when(pullRequestMapping.getCommits()).thenReturn(new RepositoryCommitMapping[] { });
        long remoteId = bitbucketPullRequest.getId();
        when(pullRequestMapping.getRemoteId()).thenReturn(remoteId);
        when(pullRequestMapping.getSourceBranch()).thenReturn("branch");
        when(pullRequestMapping.getSourceRepo()).thenReturn("sourceRepo");
        when(pullRequestMapping.getDestinationBranch()).thenReturn("master");

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
        when(repositoryPullRequestDao.saveCommit(eq(repository), anyMapOf(String.class, Object.class))).thenAnswer(new SaveCommitAnswer());

        mockPullRequestCommitsEndPoint(COMMIT_NODE);

        when(activityInfo.getPullRequest()).thenReturn(bitbucketPullRequest);
        when(bitbucketClientBuilderFactory.forRepository(repository)).then(new Answer<BitbucketClientBuilder>()
        {
            @Override
            public BitbucketClientBuilder answer(final InvocationOnMock invocation) throws Throwable
            {
                return mock(BitbucketClientBuilder.class, new BuilderAnswer());
            }
        });

        when(payload.getProgress()).thenReturn(progress);
        when(payload.getRepository()).thenReturn(repository);
        when(payload.getPageNum()).thenReturn(1);

        when(pullRequestService.createPullRequest(savePullRequestCaptor.capture())).thenAnswer(returnsFirstArg());

        when(pullRequestService.updatePullRequest(eq(pullRequestMapping.getID()), savePullRequestCaptor.capture())).thenAnswer(returnsSecondArg());

        BitbucketLinks links = mockLinks();
        when(bitbucketPullRequest.getLinks()).thenReturn(links);

        target = new RepositoryPullRequestMappingMock();
        when(repositoryPullRequestDao.createPullRequest()).thenReturn(target);
    }

    private BitbucketPullRequestCommit mockBitbucketPullRequestCommit(String node)
    {
        BitbucketPullRequestCommit remoteCommit = mock(BitbucketPullRequestCommit.class);
        when(remoteCommit.getHash()).thenReturn(node);
        BitbucketPullRequestCommitAuthor commitAuthor = mock(BitbucketPullRequestCommitAuthor.class);
        BitbucketUser user = mock(BitbucketUser.class);
        when(commitAuthor.getUser()).thenReturn(user);
        when(remoteCommit.getAuthor()).thenReturn(commitAuthor);
        return remoteCommit;
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
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), any(RepositoryPullRequestMapping.class));
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), anyMapOf(String.class, Object.class));
    }

    @Test
    public void testSourceRepositoryDeleted()
    {
        BitbucketPullRequestHead source = Mockito.mock(BitbucketPullRequestHead.class);
        when(source.getRepository()).thenReturn(null);
        when(source.getBranch()).thenReturn(null);
        when(bitbucketPullRequest.getSource()).thenReturn(source);
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).updatePullRequestInfo(anyInt(), any(RepositoryPullRequestMapping.class));
        verify(repositoryPullRequestDao, never()).savePullRequest(eq(repository), anyMapOf(String.class, Object.class));
    }

    @Test (expectedExceptions = BitbucketRequestException.Unauthorized_401.class)
    public void testAccessDenied()
    {
        when(requestor.get(anyString(), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.Unauthorized_401());
        when(cachedRequestor.get(anyString(), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.Unauthorized_401());

        testedClass.onReceive(message, payload);
    }

    @Test (expectedExceptions = BitbucketRequestException.NotFound_404.class)
    public void testNotFound()
    {
        when(requestor.get(anyString(), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.NotFound_404());
        when(cachedRequestor.get(anyString(), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.NotFound_404());

        testedClass.onReceive(message, payload);
    }

    @Test (expectedExceptions = BitbucketRequestException.InternalServerError_500.class)
    public void testInternalServerError()
    {
        when(requestor.get(anyString(), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.InternalServerError_500());
        when(cachedRequestor.get(anyString(), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenThrow(new BitbucketRequestException.InternalServerError_500());

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
    public void testEmptyTitle()
    {
        when(bitbucketPullRequest.getTitle()).thenReturn("");
        // to save new value instead update
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        testedClass.onReceive(message, payload);

        assertEquals(savePullRequestCaptor.getValue().getName(), "");
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
        when(bitbucketPullRequest.getTitle()).thenReturn(org.apache.commons.lang3.StringUtils.leftPad("title ", 1000, "long "));
        // to save new value instead update
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        testedClass.onReceive(message, payload);

        assertEquals(savePullRequestCaptor.getValue().getName(), org.apache.commons.lang3.StringUtils.leftPad("title ", 1000, "long ").substring(0, 255));
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
            participant.setApproved(i % 4 == 0);
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
        verify(cachedRequestor, times(1)).get(anyString(), anyMapOf(String.class, String.class), any(ResponseCallback.class));
    }

    @Test
    public void testNoCacheSecondPage()
    {
        when(payload.getPageNum()).thenReturn(2);
        testedClass.onReceive(message, payload);
        verify(cachedRequestor, never()).get(anyString(), anyMapOf(String.class, String.class), any(ResponseCallback.class));
    }

    @Test
    public void testCommit()
    {
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        testedClass.onReceive(message, payload);
        verify(repositoryPullRequestDao).saveCommit(eq(repository), matchMapWithNode(COMMIT_NODE));
    }

    @Test
    public void testMaxCommits()
    {
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        String[] nodes = new String[100];
        for (int i = 0; i < 100; i++)
        {
            nodes[i] = COMMIT_NODE + i;
        }
        mockPullRequestCommitsEndPoint(nodes);

        testedClass.onReceive(message, payload);
        verify(repositoryPullRequestDao, times(100)).saveCommit(eq(repository), saveCommitCaptor.capture());

        assertEquals(saveCommitCaptor.getAllValues().size(), 100);
        int i = 0;
        for (Map<String, Object> commitMap : saveCommitCaptor.getAllValues())
        {
            assertEquals(commitMap.get(RepositoryCommitMapping.NODE), COMMIT_NODE + i++);
        }
    }

    @Test
    public void testUpdateCommit()
    {
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        RepositoryCommitMapping commitMapping = mock(RepositoryCommitMapping.class);
        when(commitMapping.getNode()).thenReturn(COMMIT_NODE_ORIGINAL);
        setPRCommits(commitMapping);

        testedClass.onReceive(message, payload);

        verifyPRDaoSaveAndLinkCommit(COMMIT_NODE);
        verify(repositoryPullRequestDao).unlinkCommits(eq(repository), eq(target), argThat(containsInAnyOrder(commitMapping)));
        verify(repositoryPullRequestDao).removeCommits(argThat(containsInAnyOrder(commitMapping)));

        verifyOtherCallsOnRepositoryPullRequestDao();
    }

    @Test
    public void testUpdateCommitNoChange()
    {
        RepositoryCommitMapping newCommitMapping = mockRepositoryCommitMapping(COMMIT_NODE_ORIGINAL);
        RepositoryCommitMapping existingCommitMapping = mockRepositoryCommitMapping(COMMIT_NODE);
        setPRCommits(newCommitMapping, existingCommitMapping);

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).saveCommit(eq(repository), anyMapOf(String.class, Object.class));
        verify(repositoryPullRequestDao, never()).unlinkCommits(eq(repository), eq(target), any(Iterable.class));
        verify(repositoryPullRequestDao, never()).removeCommits(any(Iterable.class));

        verifyOtherCallsOnRepositoryPullRequestDao();
    }

    @Test
    public void testUpdateCommitRetargeted()
    {
        BitbucketPullRequestHead destinationBranch = mockRef("destinationBranch2");
        when(bitbucketPullRequest.getDestination()).thenReturn(destinationBranch);

        RepositoryCommitMapping commitMapping = mockRepositoryCommitMapping(COMMIT_NODE_ORIGINAL);
        when(pullRequestMapping.getCommits()).thenReturn(new RepositoryCommitMapping[] { commitMapping });
        setPRCommits(commitMapping);

        testedClass.onReceive(message, payload);

        verifyPRDaoSaveAndLinkCommit(COMMIT_NODE);
        verify(repositoryPullRequestDao).unlinkCommits(eq(repository), eq(target), argThat(containsInAnyOrder(commitMapping)));
        verify(repositoryPullRequestDao).removeCommits(argThat(containsInAnyOrder(commitMapping)));

        verifyOtherCallsOnRepositoryPullRequestDao();
    }

    @Test
    public void testUpdateCommitStatusChanged()
    {
        when(bitbucketPullRequest.getState()).thenReturn("merged");

        RepositoryCommitMapping commitMapping = mockRepositoryCommitMapping(COMMIT_NODE);
        setPRCommits(commitMapping);

        final BitbucketPullRequestCommit pullRequestCommit = mockBitbucketPullRequestCommit(COMMIT_NODE);
        final BitbucketPullRequestCommit pullRequestCommit2 = mockBitbucketPullRequestCommit(COMMIT_NODE_2);
        mockPullRequestCommitsEndPointWithCommits(Lists.newArrayList(pullRequestCommit2, pullRequestCommit));

        testedClass.onReceive(message, payload);

        verifyPRDaoSaveAndLinkCommit(COMMIT_NODE_2);
        verify(repositoryPullRequestDao, never()).unlinkCommits(eq(repository), eq(target), argThat(containsInAnyOrder(commitMapping)));
        verify(repositoryPullRequestDao, never()).removeCommits(argThat(containsInAnyOrder(commitMapping)));

        verifyOtherCallsOnRepositoryPullRequestDao();
    }

    @Test
    public void testUpdateCommitStatusChangedAndUpgradedFromWithoutMergeColumn()
    {
        // upgrading from 2.0.x (except 2.0.11) does not have the MERGE column in the COMMIT table
        when(bitbucketPullRequest.getState()).thenReturn("merged");

        RepositoryCommitMapping mergeCommitMapping = mockRepositoryCommitMapping(COMMIT_NODE_MERGE);
        setPRCommits(mergeCommitMapping);

        // make one remote commit a merge commit
        final BitbucketPullRequestCommit pullRequestMergeCommit = mockBitbucketPullRequestCommit(COMMIT_NODE_MERGE);
        when(pullRequestMergeCommit.getParents()).thenReturn(ImmutableList.of(mock(BitbucketNewChangeset.class), mock(BitbucketNewChangeset.class)));
        final BitbucketPullRequestCommit pullRequestNonMergeCommit = mockBitbucketPullRequestCommit(COMMIT_NODE_NON_MERGE);
        mockPullRequestCommitsEndPointWithCommits(Lists.newArrayList(pullRequestNonMergeCommit, pullRequestMergeCommit));

        testedClass.onReceive(message, payload);

        verifyPRDaoSaveAndLinkCommit(COMMIT_NODE_NON_MERGE);
        // merge commit should be re-created if existing isMerge value is different from the one from remote
        verifyPRDaoSaveAndLinkCommit(COMMIT_NODE_MERGE);

        verify(repositoryPullRequestDao).unlinkCommits(eq(repository), eq(target), argThat(containsInAnyOrder(mergeCommitMapping)));
        verify(repositoryPullRequestDao).removeCommits(argThat(containsInAnyOrder(mergeCommitMapping)));

        verifyOtherCallsOnRepositoryPullRequestDao();
    }

    @Test
    public void testUpdateCommitFallback()
    {
        when(syncDisabledHelper.isPullRequestCommitsFallback()).thenReturn(true);

        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        mockPullRequestCommitsEndPoint(COMMIT_NODE_2, COMMIT_NODE);

        RepositoryCommitMapping commitMapping1 = mockRepositoryCommitMapping(COMMIT_NODE);
        RepositoryCommitMapping commitMapping2 = mockRepositoryCommitMapping(COMMIT_NODE_ORIGINAL);
        setPRCommits(commitMapping1, commitMapping2);

        testedClass.onReceive(message, payload);
        verify(repositoryPullRequestDao, never()).saveCommit(eq(repository), matchMapWithNode(COMMIT_NODE));
        verify(repositoryPullRequestDao, never()).saveCommit(eq(repository), matchMapWithNode(COMMIT_NODE_ORIGINAL));
        verifyPRDaoSaveAndLinkCommit(COMMIT_NODE_2);

        verify(repositoryPullRequestDao, never()).unlinkCommits(eq(repository), eq(pullRequestMapping), argThat(containsInAnyOrder(commitMapping2)));
        verify(repositoryPullRequestDao, never()).removeCommits(argThat(containsInAnyOrder(commitMapping2)));

        verifyOtherCallsOnRepositoryPullRequestDao();
    }

    @Test
    public void testUpdateCommitFallbackAndUpgradedFromWithoutMergeColumn()
    {
        when(syncDisabledHelper.isPullRequestCommitsFallback()).thenReturn(true);

        // upgrading from 2.0.x (except 2.0.11) does not have the MERGE column in the COMMIT table
        when(bitbucketPullRequest.getState()).thenReturn("merged");

        RepositoryCommitMapping mergeCommitMapping = mockRepositoryCommitMapping(COMMIT_NODE_MERGE);
        RepositoryCommitMapping originalCommitMapping = mockRepositoryCommitMapping(COMMIT_NODE_ORIGINAL);
        setPRCommits(mergeCommitMapping, originalCommitMapping);

        // make one remote commit a merge commit
        final BitbucketPullRequestCommit pullRequestMergeCommit = mockBitbucketPullRequestCommit(COMMIT_NODE_MERGE);
        when(pullRequestMergeCommit.getParents()).thenReturn(ImmutableList.of(mock(BitbucketNewChangeset.class), mock(BitbucketNewChangeset.class)));
        final BitbucketPullRequestCommit pullRequestNonMergeCommit = mockBitbucketPullRequestCommit(COMMIT_NODE_NON_MERGE);
        mockPullRequestCommitsEndPointWithCommits(Lists.newArrayList(pullRequestNonMergeCommit, pullRequestMergeCommit));

        testedClass.onReceive(message, payload);

        verify(repositoryPullRequestDao, never()).saveCommit(eq(repository), matchMapWithNode(COMMIT_NODE_ORIGINAL));

        verifyPRDaoSaveAndLinkCommit(COMMIT_NODE_NON_MERGE);

        // merge and original commits not removed due to fallback
        verify(repositoryPullRequestDao, never()).unlinkCommits(eq(repository), eq(pullRequestMapping), argThat(containsInAnyOrder(mergeCommitMapping, originalCommitMapping)));
        verify(repositoryPullRequestDao, never()).removeCommits(argThat(containsInAnyOrder(mergeCommitMapping, originalCommitMapping)));

        verifyOtherCallsOnRepositoryPullRequestDao();
    }

    @Test
    public void testUpdateCommitTriggersChangedEvent()
    {
        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        RepositoryCommitMapping commitMapping = mock(RepositoryCommitMapping.class);
        when(commitMapping.getNode()).thenReturn(COMMIT_NODE_ORIGINAL);
        setPRCommits(commitMapping);

        final Integer repositoryId = 1;
        when(repository.getId()).thenReturn(repositoryId);
        final ImmutableSet<String> oldIssueKeys = ImmutableSet.of("TST-1");
        final ImmutableSet<String> newIssueKeys = ImmutableSet.of("TST-2");
        when(repositoryPullRequestDao.getIssueKeys(repository.getId(), 0)).thenReturn(oldIssueKeys).thenReturn(newIssueKeys);

        testedClass.onReceive(message, payload);

        verifyPRDaoSaveAndLinkCommit(COMMIT_NODE);
        verify(repositoryPullRequestDao).unlinkCommits(eq(repository), eq(target), argThat(containsInAnyOrder(commitMapping)));
        verify(repositoryPullRequestDao).removeCommits(argThat(containsInAnyOrder(commitMapping)));

        verify(repositoryPullRequestDao).findRequestByRemoteId(eq(repository), anyLong());
        verify(repositoryPullRequestDao).createPullRequest();
        verify(repositoryPullRequestDao).updatePullRequestIssueKeys(eq(repository), anyInt());
        verify(repositoryPullRequestDao, times(2)).getIssueKeys(anyInt(), anyInt());

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(notificationService).broadcast(eventCaptor.capture());

        IssuesChangedEvent firstEvent = (IssuesChangedEvent) eventCaptor.getValue();
        assertThat(firstEvent.getIssueKeys(), Matchers.containsInAnyOrder(new String[] { "TST-1", "TST-2" }));
    }

    @Test
    public void testStatusChanged()
    {
        BitbucketPullRequest remote = mock(BitbucketPullRequest.class);
        RepositoryPullRequestMapping local = mock(RepositoryPullRequestMapping.class);

        when(remote.getState()).thenReturn("open");
        assertTrue(testedClass.hasStatusChanged(remote, local));

        when(remote.getState()).thenReturn("open");
        when(local.getLastStatus()).thenReturn("OPEN");
        assertFalse(testedClass.hasStatusChanged(remote, local));

        when(remote.getState()).thenReturn("merged");
        when(local.getLastStatus()).thenReturn("OPEN");
        assertTrue(testedClass.hasStatusChanged(remote, local));
    }

    @Test
    public void testUpdateCommitsNumberFallback()
    {
        when(featureManager.isEnabled(eq(BitbucketSynchronizeActivityMessageConsumer.BITBUCKET_COMMITS_FALLBACK_FEATURE))).thenReturn(true);

        when(repositoryPullRequestDao.findRequestByRemoteId(eq(repository), anyLong())).thenReturn(null);

        when(pullRequestMapping.getCommits()).thenReturn(new RepositoryCommitMapping[] { });

        testedClass.onReceive(message, payload);
        verify(requestor).get(Mockito.contains("pagelen=" + BitbucketPageIterator.REQUEST_LIMIT), anyMapOf(String.class, String.class), any(ResponseCallback.class));
    }

    private void setPRCommits(final RepositoryCommitMapping... commitMappings)
    {
        when(pullRequestMapping.getCommits()).thenReturn(commitMappings);
        target.setCommits(commitMappings);
    }

    private void verifyPRDaoSaveAndLinkCommit(final String node)
    {
        verify(repositoryPullRequestDao).saveCommit(eq(repository), matchMapWithNode(node));
        verify(repositoryPullRequestDao).linkCommit(eq(repository), eq(target), matchCommitMappingWithNode(node));
    }

    private void verifyOtherCallsOnRepositoryPullRequestDao()
    {
        verify(repositoryPullRequestDao).findRequestByRemoteId(eq(repository), anyLong());
        verify(repositoryPullRequestDao).createPullRequest();
        verify(repositoryPullRequestDao).updatePullRequestIssueKeys(eq(repository), anyInt());
        verify(repositoryPullRequestDao, times(2)).getIssueKeys(anyInt(), anyInt());
        verify(notificationService).broadcast(anyObject());

        verifyNoMoreInteractions(repositoryPullRequestDao);
    }

    private RepositoryCommitMapping matchCommitMappingWithNode(final String node)
    {
        //noinspection unchecked
        return argThat(new FeatureMatcher<RepositoryCommitMapping, String>(equalTo(node), "node is", "node")
        {
            @Override
            protected String featureValueOf(final RepositoryCommitMapping actual)
            {
                return actual.getNode();
            }
        });
    }

    private Map<String, Object> matchMapWithNode(final String node)
    {
        //noinspection unchecked
        return (Map) argThat(IsMapContaining.hasEntry(RepositoryCommitMapping.NODE, node));
    }

    private void mockPullRequestCommitsEndPoint(final String... nodes)
    {
        List<BitbucketPullRequestCommit> commits = Lists.newArrayListWithExpectedSize(nodes.length);
        for (String node : nodes)
        {
            commits.add(mockBitbucketPullRequestCommit(node));
        }
        mockPullRequestCommitsEndPointWithCommits(commits);
    }

    private void mockPullRequestCommitsEndPointWithCommits(final List<BitbucketPullRequestCommit> commits)
    {
        BitbucketPullRequestPage<BitbucketPullRequestCommit> commitsPage = mock(BitbucketPullRequestPage.class);
        when(commitsPage.getValues()).thenReturn(commits);

        when(requestor.get(startsWith("commitsLink"), anyMapOf(String.class, String.class), any(ResponseCallback.class))).thenReturn(commitsPage);
    }

    private RepositoryCommitMapping mockRepositoryCommitMapping(String node)
    {
        RepositoryCommitMapping commitMapping = mock(RepositoryCommitMapping.class);
        when(commitMapping.getNode()).thenReturn(node);
        return commitMapping;
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
        return mockRef(branchName, null);
    }

    private BitbucketPullRequestHead mockRef(String branchName, String repositoryName)
    {
        BitbucketPullRequestHead source = Mockito.mock(BitbucketPullRequestHead.class);
        BitbucketBranch bitbucketBranch = new BitbucketBranch();
        bitbucketBranch.setName(branchName);
        BitbucketPullRequestRepository repository = Mockito.mock(BitbucketPullRequestRepository.class);
        when(repository.getFullName()).thenReturn(repositoryName);
        when(source.getRepository()).thenReturn(repository);
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

    /**
     * For saveCommit, returns a commit mapping that has the same NODE as in the input map. To be used in subsequent
     * linkCommit calls.
     */
    private class SaveCommitAnswer implements Answer<Object>
    {
        @Override
        public Object answer(final InvocationOnMock invocation) throws Throwable
        {
            //noinspection unchecked
            final Map<String, Object> commitMap = (Map<String, Object>) invocation.getArguments()[1];
            RepositoryCommitMapping commitMapping = mock(RepositoryCommitMapping.class);
            when(commitMapping.getNode()).thenReturn((String) commitMap.get(RepositoryCommitMapping.NODE));
            return commitMapping;
        }
    }
}
