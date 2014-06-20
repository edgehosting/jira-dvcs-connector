package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.fail;

public class MessageConsumerSupportTest
{
    private static final java.lang.Integer REPO_ID = 200;
    private static final String NODE = "e4798f084a6cf7e9aff6e8d540414ef364042a40";
    private static final String DVCS_TYPE = "my-dvcs";

    @Mock
    ChangesetService changesetService;

    @Mock
    DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Mock
    DvcsCommunicator dvcsCommunicator;

    @Mock
    Repository repository;

    MessageConsumerSupport<HasProgress> messageConsumer;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);

        when(repository.getId()).thenReturn(REPO_ID);
        when(repository.getDvcsType()).thenReturn(DVCS_TYPE);
        when(changesetService.getByNode(REPO_ID, NODE)).thenReturn(null);
        when(dvcsCommunicatorProvider.getCommunicator(anyString())).thenReturn(dvcsCommunicator);
        messageConsumer = new TestConsumer().injectMocks();
    }

    @Test
    public void syncShouldAbortIfGetChangesetReturnsWrongChangeset() throws Exception
    {
        final String returnedNode = "not-" + NODE;
        com.atlassian.jira.plugins.dvcs.model.Changeset randomChangeset = mock(com.atlassian.jira.plugins.dvcs.model.Changeset.class);
        when(randomChangeset.getNode()).thenReturn(returnedNode);
        when(dvcsCommunicator.getChangeset(repository, NODE)).thenReturn(randomChangeset);

        try
        {
            messageConsumer.onReceive(new Message<HasProgress>(), null);
            fail();
        }
        catch (SourceControlException e)
        {
            String message = e.getMessage();

            assertThat(message, containsString(DVCS_TYPE));
            assertThat(message, containsString(NODE));
            assertThat(message, containsString(returnedNode));
        }
    }

    private class TestConsumer extends MessageConsumerSupport<HasProgress>
    {
        public TestConsumer injectMocks()
        {
            changesetService = MessageConsumerSupportTest.this.changesetService;
            dvcsCommunicatorProvider = MessageConsumerSupportTest.this.dvcsCommunicatorProvider;

            return this;
        }

        @Override
        protected Repository getRepository(final HasProgress payload)
        {
            return repository;
        }

        @Override
        protected String getBranch(final HasProgress payload)
        {
            return "branch";
        }

        @Override
        protected String getNode(final HasProgress payload)
        {
            return NODE;
        }

        @Override
        protected boolean getSoftSync(final HasProgress payload)
        {
            return true;
        }

        @Override
        protected HasProgress createNextMessage(final HasProgress payload, final String parentChangesetNode)
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public String getQueue()
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public MessageAddress<HasProgress> getAddress()
        {
            throw new UnsupportedOperationException("Not implemented");
        }
    }
}
