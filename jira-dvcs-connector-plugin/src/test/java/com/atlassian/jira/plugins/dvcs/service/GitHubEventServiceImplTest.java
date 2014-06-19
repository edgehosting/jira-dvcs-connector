package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.dao.GitHubEventDAO;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.eclipse.egit.github.core.service.EventService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Date;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link com.atlassian.jira.plugins.dvcs.service.GitHubEventServiceImpl} test
 *
 */
public class GitHubEventServiceImplTest
{
    @InjectMocks
    private GitHubEventServiceImpl gitHubEventService;

    @Mock
    private Repository repository;

    @Mock
    private GithubClientProvider githubClientProvider;

    @Mock
    private GitHubEventDAO gitHubEventDAO;

    @Mock
    private ActiveObjects activeObjects;

    @Mock
    private GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator;

    @Mock
    private EventService eventService;

    @Mock
    private PageIterator<Event> events;

    private Event event1;
    private Event event2;

    @Mock
    private GitHubEventMapping savePointEvent;

    @Mock
    private GitHubEventMapping newSavePointEvent;

    @BeforeMethod
    public void init()
    {
        MockitoAnnotations.initMocks(this);

        when(githubClientProvider.getEventService(repository)).thenReturn(eventService);
        when(eventService.pageEvents(any(RepositoryId.class))).thenReturn(events);
        when(activeObjects.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
            }
        });
    }

    @Test
    public void testSynchronize()
    {
        when(gitHubEventDAO.getLastSavePoint(repository)).thenReturn(savePointEvent);
        event1 = mockEvent("4", 4);
        when(newSavePointEvent.getCreatedAt()).thenReturn(new Date(4));
        event2 = mockEvent("3", 3);
        when(savePointEvent.getCreatedAt()).thenReturn(new Date(3));
        when(gitHubEventDAO.getByGitHubId(eq(repository), eq("3"))).thenReturn(savePointEvent);
        when(gitHubEventDAO.getByGitHubId(eq(repository), eq("4"))).thenReturn(newSavePointEvent);

        when(events.iterator()).thenReturn(events);
        when(events.hasNext()).thenReturn(true, true, false);
        Collection<Event> firstPage = Lists.newArrayList(event1, event2, mockEvent("2",2));
        Collection<Event> secondPage = Lists.newArrayList(mockEvent("1", 1), mockEvent("0", 0));
        when(events.next()).thenReturn(firstPage, secondPage);

        gitHubEventService.synchronize(repository, true, new String[]{}, false);
        verify(events, times(1)).next();
        verify(gitHubEventDAO).markAsSavePoint(newSavePointEvent);
    }

    @Test
    public void testSynchronize_SavePointAtEndOfPage()
    {
        when(gitHubEventDAO.getLastSavePoint(repository)).thenReturn(savePointEvent);
        event1 = mockEvent("4", 4);
        when(newSavePointEvent.getCreatedAt()).thenReturn(new Date(4));
        event2 = mockEvent("3", 3);
        when(savePointEvent.getCreatedAt()).thenReturn(new Date(3));
        when(gitHubEventDAO.getByGitHubId(eq(repository), eq("3"))).thenReturn(savePointEvent);
        when(gitHubEventDAO.getByGitHubId(eq(repository), eq("4"))).thenReturn(newSavePointEvent);

        when(events.iterator()).thenReturn(events);
        when(events.hasNext()).thenReturn(true, true, false);
        Collection<Event> firstPage = Lists.newArrayList(event1, event2);
        Collection<Event> secondPage = Lists.newArrayList(mockEvent("2", 2), mockEvent("1", 1));
        Collection<Event> thirdPage = Lists.newArrayList(mockEvent("0", 0));
        when(events.next()).thenReturn(firstPage, secondPage, thirdPage);

        gitHubEventService.synchronize(repository, true, new String[]{}, false);
        verify(events, times(2)).next();
        verify(gitHubEventDAO).markAsSavePoint(newSavePointEvent);
    }

    private Event mockEvent(String id, long date)
    {
        Event event = mock(Event.class);
        when(event.getCreatedAt()).thenReturn(new Date(date));
        when(event.getId()).thenReturn(id);
        return event;
    }
}
