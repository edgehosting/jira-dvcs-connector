package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.model.Message;

import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.List;

@Listeners (MockitoTestNgListener.class)
public class BitbucketSynchronizeChangesetMessageConsumerTest
{
    @Mock
    private CachingDvcsCommunicator cachingCommunicator;
    @Mock
    private RepositoryService repositoryService;
    @Mock
    private LinkedIssueService linkedIssueService;
    @Mock
    private MessagingService messagingService;
    @Mock
    private ChangesetService changesetService;
    @Mock
    private BitbucketCommunicator communicator;
    @InjectMocks
    public BitbucketSynchronizeChangesetMessageConsumer messageConsumer;

    int softSync = MessagingService.DEFAULT_PRIORITY;
    @Mock
    MessageAddress<BitbucketSynchronizeChangesetMessage> messageAddress;
    @Captor
    private ArgumentCaptor<BitbucketSynchronizeChangesetMessage> changesetMessageCaptor;

    private Date lastCommitDate = new Date();
    private Date reallyOldCommitDate = new Date(0);
    private int repoId = 1234;
    private BitbucketNewChangeset newChangeset1;
    private BitbucketNewChangeset newChangeset2;
    private Progress progress;
    private Date refreshAfterSynchronizedAt;

    private boolean isSoftSync = false;
    private boolean isWebHookSync = false;
    private int syncAuditId = 0;

    private static final List<String>excludeNodes = Arrays.asList("excludednode1", "excludednode2");
    private static final List<String>includeNodes = Arrays.asList("includednode1", "includednode2");

    private Set<String> referencedProjects = new HashSet<String>();

    private static final BitbucketChangesetPage secondToLastChangesetPage = new BitbucketChangesetPage();

    private static final BitbucketChangesetPage lastChangesetPage = new BitbucketChangesetPage();
    private static final BitbucketChangesetPage thirdToLastChangesetPage = new BitbucketChangesetPage();

    private BitbucketSynchronizeChangesetMessage thirdToLastmessage;
    private BitbucketSynchronizeChangesetMessage secondToLastmessage;
    private BitbucketSynchronizeChangesetMessage lastmessage;
    private Message<BitbucketSynchronizeChangesetMessage> message;

    private Repository repository;

    @BeforeMethod
    public void setUp() throws Exception
    {
        repository = new Repository();
        repository.setId(repoId);
        repository.setLinkUpdateAuthorised(true);
        progress = new DefaultProgress();
        refreshAfterSynchronizedAt = new Date();
        secondToLastmessage = setUpChangesetMessage(secondToLastChangesetPage);
        lastmessage = setUpChangesetMessage(lastChangesetPage);
        thirdToLastmessage = setUpChangesetMessage(thirdToLastChangesetPage);
        message = new Message<BitbucketSynchronizeChangesetMessage>();
        setUpChangesetPages();
        when(cachingCommunicator.getDelegate()).thenReturn(communicator);
        when(messagingService.get(eq(BitbucketSynchronizeChangesetMessage.class), anyString())).thenReturn(messageAddress);
        when(communicator.getNextPage(eq(repository),
                eq(includeNodes), eq(excludeNodes), any(BitbucketChangesetPage.class))).thenReturn(lastChangesetPage);


    }

    @Test
    public void testOnReceiveLastMessage() throws Exception
    {
        when(communicator.getNextPage(any(Repository.class),
                eq(includeNodes), eq(excludeNodes), eq(secondToLastChangesetPage))).thenReturn(lastChangesetPage);
        when(changesetService.getByNode(eq(repoId), anyString())).thenReturn(null); //changeset is not already in the database
        when(changesetService.findReferencedProjects(repoId)).thenReturn(referencedProjects);
        messageConsumer.onReceive(message, secondToLastmessage);

        verify(cachingCommunicator).linkRepository(repository, referencedProjects);
        verifyNoMoreInteractions(messagingService);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOnReceiveSecondToLastMessage() throws Exception
    {
        when(communicator.getNextPage(any(Repository.class),
                eq(includeNodes), eq(excludeNodes), eq(thirdToLastChangesetPage))).thenReturn(secondToLastChangesetPage);
        when(changesetService.getByNode(eq(repoId), anyString())).thenReturn(null);
        when(changesetService.findReferencedProjects(eq(repoId))).thenReturn(referencedProjects);
        messageConsumer.onReceive(message, thirdToLastmessage);

        verify(cachingCommunicator, never()).linkRepository(any(Repository.class), any(Set.class));
        verify(messagingService).publish(eq(messageAddress), changesetMessageCaptor.capture(), eq(softSync));
        assertEquals(refreshAfterSynchronizedAt, changesetMessageCaptor.getValue().getRefreshAfterSynchronizedAt());
        assertEquals(progress, changesetMessageCaptor.getValue().getProgress());
    }

    @Test
    public void testOnReceiveWhenRepoLastCommitDateNonexistent() throws Exception
    {
        when(changesetService.getByNode(eq(repoId), anyString())).thenReturn(null);
        messageConsumer.onReceive(message, secondToLastmessage);

        verify(repositoryService).save(repository);
    }

    @Test
    public void testOnReceiveWhenRepoLastCommitDateOld() throws Exception
    {
        repository.setLastCommitDate(reallyOldCommitDate);
        when(changesetService.getByNode(eq(repoId), anyString())).thenReturn(null);
        messageConsumer.onReceive(message, secondToLastmessage);

        verify(repositoryService).save(repository);
    }

    @Test
    public void testOnReceiveWhenRepoLastCommitNonStale() throws Exception
    {
        repository.setLastCommitDate(new Date());
        when(changesetService.getByNode(anyInt(), anyString())).thenReturn(null);
        messageConsumer.onReceive(message, lastmessage);

        verify(repositoryService, never()).save(repository);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testOnReceiveWhenChangesetAlreadySeenEarlier() throws Exception
    {
        when(changesetService.getByNode(anyInt(), anyString())).thenReturn(
                new Changeset(1, "a changeset that's already in the db", "", new Date()));
        messageConsumer.onReceive(message, lastmessage);

        verify(changesetService, never()).create(any(Changeset.class), any(Set.class));
    }

    private void setUpChangesetPages()
    {
        newChangeset1 = new BitbucketNewChangeset();
        newChangeset2 = new BitbucketNewChangeset();

        newChangeset1.setParents(new ArrayList<BitbucketNewChangeset>());
        newChangeset2.setParents(new ArrayList<BitbucketNewChangeset>());

        newChangeset1.setDate(lastCommitDate);
        newChangeset2.setDate(lastCommitDate);

        secondToLastChangesetPage.setNext("a string whose presence indicates that this is not the last page");

        ArrayList<BitbucketNewChangeset> newChangesets1 = new ArrayList<BitbucketNewChangeset>();
        ArrayList<BitbucketNewChangeset> newChangesets2 = new ArrayList<BitbucketNewChangeset>();

        newChangesets1.add(newChangeset1);
        newChangesets2.add(newChangeset2);

        secondToLastChangesetPage.setValues(newChangesets1);
        lastChangesetPage.setValues(newChangesets2);
    }

    private BitbucketSynchronizeChangesetMessage setUpChangesetMessage(BitbucketChangesetPage changesetPage)
    {
        return new BitbucketSynchronizeChangesetMessage(repository,
                refreshAfterSynchronizedAt,
                progress,
                includeNodes,
                excludeNodes,
                changesetPage,
                new HashMap<String, String>(),
                isSoftSync,
                syncAuditId,
                isWebHookSync);
    }
}