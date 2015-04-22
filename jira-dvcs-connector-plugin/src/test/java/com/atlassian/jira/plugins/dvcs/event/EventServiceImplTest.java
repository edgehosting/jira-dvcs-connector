package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.dao.StreamCallback;
import com.atlassian.jira.plugins.dvcs.dao.ao.EntityBeanGenerator;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class EventServiceImplTest
{
    private static final int REPO_1_ID = 100;
    private static final int REPO_2_ID = 200;

    @Mock
    private Repository repo1;

    @Mock
    private Repository repo2;

    private SyncEventMapping repo1Mapping;
    private SyncEventMapping repo2Mapping;
    private SyncEventMapping repo2BadMapping;

    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private SyncEventDao syncEventDao;

    @Mock
    private EventLimiterFactory eventLimiterFactory;

    @Mock
    private EventLimiter eventLimiter;

    private EventServiceImpl eventService;

    @BeforeMethod
    public void setUp() throws Exception
    {
        eventService = new EventServiceImpl(eventPublisher, syncEventDao, eventLimiterFactory, MoreExecutors.sameThreadExecutor());

        when(eventLimiterFactory.create()).thenReturn(eventLimiter);
        when(syncEventDao.create()).then(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                return createMapping();
            }
        });

        when(repo1.getId()).thenReturn(REPO_1_ID);
        when(repo2.getId()).thenReturn(REPO_2_ID);

        repo1Mapping = createMapping(repo1, "repo1-mapping");
        repo2Mapping = createMapping(repo2, "repo2-mapping");
        repo2BadMapping = createMapping(repo2, "repo2-bad-mapping");
        repo2BadMapping.setEventClass("com.does.not.Exist");

        doAnswer(new StreamAnswer(repo1Mapping)).when(syncEventDao).streamAllByRepoId(eq(REPO_1_ID), any(StreamCallback.class));
        doAnswer(new StreamAnswer(repo2BadMapping, repo2Mapping)).when(syncEventDao).streamAllByRepoId(eq(REPO_2_ID), any(StreamCallback.class));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void storeEventThrowsExceptionEventThatIsNotMarshallable() throws Exception
    {
        eventService.storeEvent(repo1, new BadEvent(), false);
    }

    @Test
    public void storeSavesMappingInDao() throws Exception
    {
        final TestEvent event = new TestEvent(new Date(0), "my-data");
        final boolean scheduled = true;
        eventService.storeEvent(repo1, event, scheduled);

        ArgumentCaptor<SyncEventMapping> captor = ArgumentCaptor.forClass(SyncEventMapping.class);
        verify(syncEventDao).save(captor.capture());

        SyncEventMapping mapping = captor.getValue();
        assertThat(mapping, notNullValue());
        assertThat(mapping.getRepoId(), equalTo(repo1.getId()));
        assertThat(mapping.getEventDate(), equalTo(event.getDate()));
        assertThat(mapping.getEventClass(), equalTo(event.getClass().getName()));
        assertThat(mapping.getEventJson(), equalTo("{\"date\":0,\"data\":\"my-data\"}"));
        assertThat(mapping.getScheduledSync(), equalTo(scheduled));
    }

    @Test
    public void dispatchShouldReadMappingsFromDaoAndSendToEventPublisher() throws Exception
    {
        eventService.dispatchEvents(repo1);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());

        Object published = captor.getValue();
        assertThat(published, instanceOf(TestEvent.class));
        assertThat((TestEvent) published, equalTo(new TestEvent(new Date(repo1.getId()), "repo1-mapping")));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void dispatchShouldDeleteMappingOnceItHasBeenPublished() throws Exception
    {
        eventService.dispatchEvents(repo2);

        // both events should be deleted
        ArgumentCaptor<SyncEventMapping> captor = ArgumentCaptor.forClass(SyncEventMapping.class);
        verify(syncEventDao, times(2)).delete(captor.capture());

        List<SyncEventMapping> deletedMappings = captor.getAllValues();
        assertThat(deletedMappings.get(0), is(repo2BadMapping));
        assertThat(deletedMappings.get(1), is(repo2Mapping));
    }

    @Test
    public void dispatchShouldBeResilientToMissingClasses() throws Exception
    {
        eventService.dispatchEvents(repo2);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publish(captor.capture());

        Object published = captor.getValue();
        assertThat(published, instanceOf(TestEvent.class));
        assertThat((TestEvent) published, equalTo(new TestEvent(new Date(repo2.getId()), "repo2-mapping")));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void dispatchShouldTakeEventLimitsIntoAccount() throws Exception
    {
        final SyncEventMapping repo1Mapping2 = createMapping(repo1, "repo1-mapping2");

        // let the first event through and limit the 2nd
        doAnswer(new StreamAnswer(repo1Mapping, repo1Mapping2)).when(syncEventDao).streamAllByRepoId(eq(REPO_1_ID), any(StreamCallback.class));
        when(eventLimiter.isLimitExceeded(any(SyncEvent.class), anyBoolean())).thenReturn(false, true);
        when(eventLimiter.getLimitExceededCount()).thenReturn(1);

        eventService.dispatchEvents(repo1);

        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher, times(2)).publish(captor.capture());

        List<Object> published = captor.getAllValues();
        assertThat(published.size(), equalTo(2));
        assertThat("first event should be published", published.get(0), instanceOf(TestEvent.class));
        assertThat("limit exceeded event should be raised", published.get(1), instanceOf(LimitExceededEvent.class));
        assertThat("event should contain dropped event count", ((LimitExceededEvent) published.get(1)).getDroppedEventCount(), equalTo(1));
        verifyNoMoreInteractions(eventPublisher);
    }

    @Test
    public void destroyShouldShutdownExecutor() throws Exception
    {
        BlockingQueue<Runnable> queue = mock(BlockingQueue.class);
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
        when(executor.getQueue()).thenReturn(queue);

        EventServiceImpl eventService = new EventServiceImpl(eventPublisher, syncEventDao, eventLimiterFactory, executor);
        eventService.destroy();

        verify(executor).shutdown();
        verify(executor, never()).shutdownNow();
        verify(queue).clear();
        verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    private SyncEventMapping createMapping()
    {
        return new EntityBeanGenerator().createInstanceOf(SyncEventMapping.class);
    }

    private SyncEventMapping createMapping(Repository repository, String data)
    {
        Date date = new Date(repository.getId());

        SyncEventMapping mapping = createMapping();
        mapping.setRepoId(repository.getId());
        mapping.setEventDate(date);
        mapping.setEventClass(TestEvent.class.getName());
        mapping.setEventJson(String.format("{\"date\":%d,\"data\": \"%s\"}", repository.getId(), data));
        mapping.setScheduledSync(false);

        return mapping;
    }

    /**
     * Answer for streaming mappings.
     */
    private static class StreamAnswer implements Answer<Void>
    {
        private final ImmutableList<SyncEventMapping> mappings;

        public StreamAnswer(SyncEventMapping... mappings)
        {
            this.mappings = ImmutableList.copyOf(mappings);
        }

        @Override
        public Void answer(final InvocationOnMock invocation) throws Throwable
        {
            final StreamCallback callback = (StreamCallback) invocation.getArguments()[1];

            for (SyncEventMapping mapping : mappings)
            {
                callback.callback(mapping);
            }

            return null;
        }
    }
}
