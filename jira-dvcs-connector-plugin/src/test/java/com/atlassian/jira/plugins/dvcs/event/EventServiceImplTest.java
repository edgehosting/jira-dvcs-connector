package com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.dao.ao.EntityBeanGenerator;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.List;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class EventServiceImplTest
{
    @Mock
    Repository repo1;

    @Mock
    Repository repo2;

    SyncEventMapping repo1Mapping;

    SyncEventMapping repo2Mapping;
    SyncEventMapping repo2BadMapping;

    @Mock
    EventPublisher eventPublisher;

    @Mock
    SyncEventDao syncEventDao;

    @InjectMocks
    EventServiceImpl eventService;

    @BeforeMethod
    public void setUp() throws Exception
    {
        eventService = new EventServiceImpl(eventPublisher, syncEventDao, MoreExecutors.sameThreadExecutor());

        when(syncEventDao.create()).then(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                return createMapping();
            }
        });

        when(repo1.getId()).thenReturn(100);
        when(repo2.getId()).thenReturn(200);

        repo1Mapping = createMapping(repo1, "repo1-mapping");
        repo2Mapping = createMapping(repo2, "repo2-mapping");
        repo2BadMapping = createMapping(repo2, "repo2-bad-mapping");
        repo2BadMapping.setEventClass("com.does.not.Exist");

        when(syncEventDao.findAllByRepoId(repo1.getId())).thenReturn(ImmutableList.of(repo1Mapping));
        when(syncEventDao.findAllByRepoId(repo2.getId())).thenReturn(ImmutableList.of(repo2BadMapping, repo2Mapping));
    }

    @Test (expectedExceptions = IllegalArgumentException.class)
    public void storeEventThrowsExceptionEventThatIsNotMarshallable() throws Exception
    {
        eventService.storeEvent(repo1, new BadEvent());
    }

    @Test
    public void storeSavesMappingInDao() throws Exception
    {
        TestEvent event = new TestEvent(new Date(0), "my-data");
        eventService.storeEvent(repo1, event);

        ArgumentCaptor<SyncEventMapping> captor = ArgumentCaptor.forClass(SyncEventMapping.class);
        verify(syncEventDao).save(captor.capture());

        SyncEventMapping mapping = captor.getValue();
        assertThat(mapping, notNullValue());
        assertThat(mapping.getRepoId(), equalTo(repo1.getId()));
        assertThat(mapping.getEventDate(), equalTo(event.getDate()));
        assertThat(mapping.getEventClass(), equalTo(event.getClass().getName()));
        assertThat(mapping.getEventJson(), equalTo("{\"date\":0,\"data\":\"my-data\"}"));
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

        return mapping;
    }
}
