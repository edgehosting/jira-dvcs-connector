package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.SimpleClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.DvcsErrorMessages;
import com.atlassian.jira.plugins.dvcs.event.RepositorySync;
import com.atlassian.jira.plugins.dvcs.event.RepositorySyncHelper;
import com.atlassian.jira.plugins.dvcs.event.ThreadEventsCaptor;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.BaseProgressEnabledMessage;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubRateLimitExceededException;
import com.atlassian.jira.plugins.dvcs.spi.github.RateLimit;
import com.atlassian.jira.plugins.dvcs.util.SameThreadExecutor;
import com.atlassian.jira.util.I18nHelper;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.EnumSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.springframework.test.util.ReflectionTestUtils.setField;
import static org.testng.Assert.assertEquals;

public class MessageExecutorTest
{
    private static final MessageAddress<MockPayload> MSG_ADDRESS = new MockAddress();
    private static final String GITHUB_RATE_LIMIT_REACHED_ERROR = "GITHUB_RATE_LIMIT_REACHED_ERROR";

    @Mock
    private Repository repository;

    private ClusterLockService clusterLockService = new SimpleClusterLockService();

    @Mock
    private MessageConsumer consumer;

    @Mock
    private ClusterLockServiceFactory clusterLockServiceFactory;

    @Mock
    private MessagingService messagingService;

    @Mock
    private RepositorySyncHelper repoSyncHelper;

    @Mock
    private RepositorySync repoSync;

    @Mock
    private ThreadEventsCaptor threadEventsCaptor;

    @Mock
    private I18nHelper i18nHelper;

    @InjectMocks
    private MessageExecutor messageExecutor;

    private static final RateLimit rateLimit = new RateLimit(10, 0, System.currentTimeMillis());

    @BeforeMethod
    public void setUp() throws Exception
    {
        // create and inject the MessageExecutor
        messageExecutor = new MessageExecutor(new SameThreadExecutor());
        initMocks(this);
        setField(messageExecutor, "consumers", new MessageConsumer<?>[] { consumer });

        when(consumer.getAddress()).thenReturn(MSG_ADDRESS);
        when(consumer.getParallelThreads()).thenReturn(1);

        when(clusterLockServiceFactory.getClusterLockService()).thenReturn(clusterLockService);
        when(repoSyncHelper.startSync(any(Repository.class), any(EnumSet.class))).thenReturn(repoSync);

        messageExecutor.init();
    }

    @AfterMethod
    public void tearDown() throws Exception
    {
        messageExecutor.destroy();
    }

    @Test
    public void executorShouldTryToEndProgressAfterProcessingSmartCommits() throws Exception
    {
        final MockPayload payload = new MockPayload();
        final Message<MockPayload> message = createMessage();
        setupDataForMessageRunnable(payload, message);

        // get the consumer to check the queue
        messageExecutor.notify(MSG_ADDRESS.getId());

        // the executor must store the events before trying to end progress
        InOrder inOrder = inOrder(repoSync, messagingService);
        inOrder.verify(repoSync).finish();
        inOrder.verify(messagingService).tryEndProgress(repository, payload.getProgress(), consumer, 0);
    }

    @Test
    public void executorShouldTryToEndProgressAfterHandlingGithubRateLimitExceededError() throws Exception
    {
        final MockPayload payload = new MockPayload();
        final Message<MockPayload> message = createMessage();
        setupDataForMessageRunnable(payload, message);

        doThrow(new GithubRateLimitExceededException(rateLimit)).when(consumer).onReceive(message, payload);
        when(i18nHelper.getText(eq(DvcsErrorMessages.GITHUB_RATE_LIMIT_REACHED_ERROR_KEY), anyLong())).thenReturn(GITHUB_RATE_LIMIT_REACHED_ERROR);
                // get the consumer to check the queue
                messageExecutor.notify(MSG_ADDRESS.getId());

        // the executor must store the events before trying to end progress
        InOrder inOrder = inOrder(repoSync, messagingService);
        inOrder.verify(repoSync).finish();
        inOrder.verify(messagingService).tryEndProgress(repository, payload.getProgress(), consumer, 0);

        assertEquals(payload.getProgress().getError(), GITHUB_RATE_LIMIT_REACHED_ERROR);
    }

    @Test
    public void destroyShouldShutdownExecutor() throws Exception
    {
        BlockingQueue<Runnable> queue = mock(BlockingQueue.class);
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
        when(executor.getQueue()).thenReturn(queue);

        MessageExecutor messageExecutor = new MessageExecutor(executor);
        messageExecutor.destroy();

        verify(executor).shutdown();
        verify(executor, never()).shutdownNow();
        verify(queue).clear();
        verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    private void setupDataForMessageRunnable(final MockPayload payload, final Message<MockPayload> message)
    {
        when(messagingService.getNextMessageForConsuming(consumer, MSG_ADDRESS.getId())).thenReturn(message, (Message) null);
        when(messagingService.deserializePayload(message)).thenReturn(payload);
        when(messagingService.getRepositoryFromMessage(message)).thenReturn(repository);
    }

    private Message<MockPayload> createMessage()
    {
        Message<MockPayload> message = new Message<MockPayload>();
        message.setAddress(MSG_ADDRESS);
        message.setPayload("{}");
        message.setPayloadType(MockPayload.class);
        message.setTags(new String[] { });
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
            super(new DefaultProgress(), 1, true, repository, false);
            getProgress().setSoftsync(true);
        }
    }
}
