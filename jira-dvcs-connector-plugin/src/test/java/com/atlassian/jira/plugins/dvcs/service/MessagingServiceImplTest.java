package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.cache.memory.MemoryCacheManager;
import com.atlassian.jira.plugins.dvcs.dao.MessageDao;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.event.CarefulEventService;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.HasProgress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.smartcommits.SmartcommitsChangesetsProcessor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeActivityMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.google.common.util.concurrent.FutureCallback;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MessagingServiceImplTest
{
    static final int REPO_ID = 123;

    Promise<Void> smartCommitsPromise;
    Progress progress;
    FutureCallback<Void> mockDispatchCallback;

    @Mock
    Repository repository;

    @Mock
    SmartcommitsChangesetsProcessor smartcCommitsProcessor;

    @Mock
    Synchronizer synchronizer;

    @Mock
    HttpClientProvider httpClientProvider;

    @Mock
    ChangesetService changesetService;

    @Mock
    CarefulEventService eventService;

    @Mock
    MessageDao messageDao;

    @Mock
    SyncAuditLogDao syncAudit;

    @Mock
    MessageConsumer<? extends HasProgress> consumer = mock(MessageConsumer.class);

    @InjectMocks
    MessagingServiceImpl messagingService;

    @BeforeMethod
    public void setUpMessagingService() throws Exception
    {
        messagingService = new MessagingServiceImpl(new MemoryCacheManager());
        MockitoAnnotations.initMocks(this);
    }

    @BeforeMethod
    public void setUpOtherMocks() throws Exception
    {
        progress = new DefaultProgress();
        mockDispatchCallback = mock(FutureCallback.class);
        smartCommitsPromise = Promises.promise(null);

        when(repository.getId()).thenReturn(REPO_ID);
        when(messageDao.getMessagesForConsumingCount(MessagingServiceImpl.SYNCHRONIZATION_REPO_TAG_PREFIX + REPO_ID)).thenReturn(0);
        when(smartcCommitsProcessor.startProcess(progress, repository, changesetService)).thenReturn(smartCommitsPromise);
    }

    @Test
    public void getReturnsDifferentInstancesGivenDifferentKeys() throws Exception
    {
        final MessageAddress<BitbucketSynchronizeActivityMessage> address = this.messagingService.get(BitbucketSynchronizeActivityMessage.class, BitbucketSynchronizeActivityMessageConsumer.KEY);
        final MessageAddress<BitbucketSynchronizeActivityMessage> address2 = this.messagingService.get(BitbucketSynchronizeActivityMessage.class, "abc");
        assertThat(address, not(sameInstance(address2)));
    }

    @Test
    public void getReturnsSameInstanceGivenSameKey() throws Exception
    {
        final MessageAddress<BitbucketSynchronizeActivityMessage> address = messagingService.get(BitbucketSynchronizeActivityMessage.class, BitbucketSynchronizeActivityMessageConsumer.KEY);
        final MessageAddress<BitbucketSynchronizeActivityMessage> address2 = messagingService.get(BitbucketSynchronizeActivityMessage.class, BitbucketSynchronizeActivityMessageConsumer.KEY);
        assertThat(address, sameInstance(address2));
    }

    @Test
    public void getReturnsSameInstanceGivenSameKeyButDifferentPayloadType() throws Exception
    {
        final MessageAddress<BitbucketSynchronizeActivityMessage> address = messagingService.get(BitbucketSynchronizeActivityMessage.class, BitbucketSynchronizeActivityMessageConsumer.KEY);
        final MessageAddress<?> address2 = messagingService.get(BitbucketSynchronizeChangesetMessage.class, BitbucketSynchronizeActivityMessageConsumer.KEY);
        //noinspection unchecked
        assertThat(address, sameInstance((MessageAddress<BitbucketSynchronizeActivityMessage>) address2));
    }

    @Test
    public void tryEndProgressShouldDispatchEventsWhenSmartCommitsProcessingSucceeds() throws Exception
    {
        messagingService.tryEndProgress(repository, progress, consumer, 456);
        verify(eventService).dispatchEvents(repository);
    }

    @Test
    public void tryEndProgressShouldDispatchEventsAfterSmartCommitsProcessingIsDone() throws Exception
    {
        smartCommitsPromise = Promises.toRejectedPromise(new Throwable(), Void.class);

        messagingService.tryEndProgress(repository, progress, consumer, 456);
        verify(eventService).dispatchEvents(repository);
    }

    @Test
    public void tryEndProgressShouldNotDispatchEventsWhenThereIsASyncError() throws Exception
    {
        progress.setError("bad juju");

        messagingService.tryEndProgress(repository, progress, consumer, 456);
        verify(eventService, never()).dispatchEvents(repository);
    }
}
