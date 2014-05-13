package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.SimpleClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.event.ThreadEventsCaptor;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.google.common.util.concurrent.MoreExecutors;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;

//@Listeners (MockitoTestNgListener.class)
public class MessageExecutorTest
{
    static final MessageAddress<MockPayload> MSG_ADDRESS = new MockAddress();

    @Mock
    Repository repository;

    ClusterLockService clusterLockService = new SimpleClusterLockService();

    @Mock
    MessageConsumer consumer;

    @Mock
    ClusterLockServiceFactory clusterLockServiceFactory;

    @Mock
    MessagingService messagingService;

    @Mock
    ThreadEvents threadEvents;

    @Mock
    ThreadEventsCaptor threadEventsCaptor;

    @InjectMocks
    MessageExecutor messageExecutor;

    @BeforeMethod
    public void setUp() throws Exception
    {
        // create and inject the MessageExecutor
        messageExecutor = new MessageExecutor(MoreExecutors.sameThreadExecutor());
        initMocks(this);
        setField(messageExecutor, "consumers", new MessageConsumer<?>[] { consumer });

        when(consumer.getAddress()).thenReturn(MSG_ADDRESS);
        when(consumer.getParallelThreads()).thenReturn(1);

        when(clusterLockServiceFactory.getClusterLockService()).thenReturn(clusterLockService);
        when(threadEvents.startCapturing()).thenReturn(threadEventsCaptor);

        messageExecutor.init();
    }

    @AfterMethod
    public void tearDown() throws Exception
    {
        messageExecutor.destroy();
    }

    @Test
    public void executorShouldPublishEventsAfterProcessingSmartCommits() throws Exception
    {
        final MockPayload payload = new MockPayload();
        final Message<MockPayload> message = createMessage();

        when(messagingService.getNextMessageForConsuming(consumer, MSG_ADDRESS.getId())).thenReturn(message, (Message) null);
        when(messagingService.deserializePayload(message)).thenReturn(payload);
        when(messagingService.getRepositoryFromMessage(message)).thenReturn(repository);

        // get the consumer to check the queue
        messageExecutor.notify(MSG_ADDRESS.getId());

        // smart commits are processed in the messaging service, which should be called before sending events
        InOrder inOrder = inOrder(messagingService, threadEventsCaptor);
        inOrder.verify(messagingService).tryEndProgress(eq(repository), any(Progress.class), eq(consumer), anyInt());
        inOrder.verify(threadEventsCaptor).sendToEventPublisher();
    }

    private Message<MockPayload> createMessage()
    {
        Message<MockPayload> message = new Message<MockPayload>();
        message.setAddress(MSG_ADDRESS);
        message.setPayload("{}");
        message.setPayloadType(MockPayload.class);
        message.setTags(new String[] {});
        message.setPriority(0);

        return message;
    }

    private static class MockAddress implements MessageAddress<MockPayload>
    {
        @Override
        public String getId()
        {
            return "test-id";
        }

        @Override
        public Class<MockPayload> getPayloadType()
        {
            return MockPayload.class;
        }
    }

    private class MockPayload extends BaseProgressEnabledMessage
    {
        MockPayload()
        {
            super(new DefaultProgress(), 1, true, repository);
            getProgress().setSoftsync(true);
        }
    }
}
