package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueServiceImpl;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeChangesetMessageConsumer;
import com.google.common.collect.Lists;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public final class TestSmartcommitsOnBitbucketMessageConsumer
{
    @Mock
    private Repository repositoryMock;

    @Mock
    private Message<BitbucketSynchronizeChangesetMessage> messageMock;

    @Mock
    private BitbucketSynchronizeChangesetMessage payloadMock;

    @Mock
    private RepositoryService repositoryServiceMock;

    @Mock
    private CachingDvcsCommunicator cachingCommunicatorMock;

    @Spy
    private final LinkedIssueService linkedIssueServiceSpy = new LinkedIssueServiceImpl();

    @Mock
    private ChangesetService changesetServiceMock;

    @Mock
    private MessagingService messagingServiceMock;

    @Mock
    private BitbucketCommunicator communicatorMock;

    @Mock
    private Progress progressMock;

    @Captor
    private ArgumentCaptor<Changeset> savedChangesetCaptor;

    @InjectMocks
    private BitbucketSynchronizeChangesetMessageConsumer consumer;

    private Changeset changesetWithJIRAIssueMock;

    @BeforeMethod
    private void initializeMocks()
    {
        MockitoAnnotations.initMocks(this);
        when(repositoryServiceMock.get(repositoryMock.getId())).thenReturn(repositoryMock);
    }

    @Test
    public void foundIssueKey_ShouldMarkSmartcommit() throws InterruptedException
    {
        prepare("message MES-123 text");
        when(repositoryMock.isSmartcommitsEnabled()).thenReturn(true);

        consumer.onReceive(messageMock, payloadMock);

        verify(changesetServiceMock).create(savedChangesetCaptor.capture(), anySetOf(String.class));
        assertTrue("Smart commit should be available.", savedChangesetCaptor.getValue().isSmartcommitAvaliable());
    }

    @Test
    public void notFoundIssueKey_ShouldNotMarkSmartcommit() throws InterruptedException
    {
        prepare("message text no issue key");
        when(repositoryMock.isSmartcommitsEnabled()).thenReturn(true);

        consumer.onReceive(messageMock, payloadMock);

        verify(changesetServiceMock).create(savedChangesetCaptor.capture(), anySetOf(String.class));
        assertFalse("Smart commit should not be available.", savedChangesetCaptor.getValue().isSmartcommitAvaliable());
    }

    @Test
    public void issueKeyFoundSmartcommitsDisabled_ShouldNotMarkSmartcommit() throws InterruptedException
    {
        prepare("message MES-123 issue key");
        when(repositoryMock.isSmartcommitsEnabled()).thenReturn(false);

        consumer.onReceive(messageMock, payloadMock);

        verify(changesetServiceMock).create(savedChangesetCaptor.capture(), anySetOf(String.class));
        assertTrue("Smart commit should not be available.", (savedChangesetCaptor.getValue().isSmartcommitAvaliable() == null)
                || !savedChangesetCaptor.getValue().isSmartcommitAvaliable());
    }

    protected void prepare(final String msg)
    {
        when(cachingCommunicatorMock.getDelegate()).thenReturn(communicatorMock);
        when(payloadMock.getRepository()).thenReturn(repositoryMock);
        when(communicatorMock.getNextPage(eq(repositoryMock), anyListOf(String.class), anyListOf(String.class), any(BitbucketChangesetPage.class)))
                .thenReturn(samplePage(msg));
        when(payloadMock.isSoftSync()).thenReturn(true);
        changesetWithJIRAIssueMock = changesetWithMessage(msg);
        when(payloadMock.getProgress()).thenReturn(progressMock);
    }

    private BitbucketChangesetPage samplePage(final String msg)
    {
        final BitbucketChangesetPage page = new BitbucketChangesetPage();
        page.setValues(Lists.newArrayList(changesetModelWithMessage(msg)));
        return page;
    }

    private BitbucketNewChangeset changesetModelWithMessage(final String msg)
    {
        final BitbucketNewChangeset cset = new BitbucketNewChangeset();
        cset.setMessage(msg);
        cset.setHash("node");
        cset.setParents(Lists.newArrayList(parentNode("parent-node")));
        return Mockito.spy(cset);
    }

    private Changeset changesetWithMessage(final String msg)
    {
        return new Changeset(0, "node", msg, new Date());
    }

    private BitbucketNewChangeset parentNode(final String string)
    {
        final BitbucketNewChangeset node = new BitbucketNewChangeset();
        node.setHash(string);
        return node;
    }

}
