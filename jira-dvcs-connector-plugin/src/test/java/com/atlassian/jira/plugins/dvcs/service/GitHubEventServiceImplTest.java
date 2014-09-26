package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.dao.GitHubEventDAO;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestPageMessage;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.PageIterator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.eclipse.egit.github.core.service.EventService;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * {@link com.atlassian.jira.plugins.dvcs.service.GitHubEventServiceImpl} test
 * <p/>
 * Note that these tests DO NOT actually test that the events are processed, they are testing the overall synchronize
 * flow and that the pages are fetched etc.
 * <p/>
 * Be aware that generally the id of the mock events such as #event1 need to line up with the id that is used to
 * retrieve the DB mapping class to make the tests work. Obvious but easy to forget.
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
    private EventService eventService;

    @Mock
    private PageIterator<Event> events;

    @Mock
    private GitHubEventMapping savePointEvent;

    @Mock
    private GitHubEventMapping newSavePointEvent;

    @Mock
    private SyncDisabledHelper syncDisabledHelper;

    @Mock
    private Synchronizer synchronizer;

    @Mock
    private Progress progress;

    @Mock
    private MessagingService messagingService;

    @BeforeMethod
    public void init()
    {
        MockitoAnnotations.initMocks(this);

        when(githubClientProvider.getEventService(repository)).thenReturn(eventService);
        when(eventService.pageEvents(any(RepositoryId.class))).thenReturn(events);

        when(synchronizer.getProgress(eq(repository.getId()))).thenReturn(progress);
    }

    @Test
    public void testDoesNothingWhenNothingToProcess()
    {

        when(gitHubEventDAO.getLastSavePoint(repository)).thenReturn(savePointEvent);

        when(events.iterator()).thenReturn(events);
        when(events.next()).thenReturn(new LinkedList<Event>());
        when(events.hasNext()).thenReturn(false);

        gitHubEventService.synchronize(repository, true, new String[] { }, false);

        verify(gitHubEventDAO).getLastSavePoint(eq(repository));
        verifyNoMoreInteractions(gitHubEventDAO);
        verifyNoMoreInteractions(messagingService);
    }

    @Test
    public void testDoesNothingWhenStopIsTrue()
    {

        when(gitHubEventDAO.getLastSavePoint(repository)).thenReturn(savePointEvent);
        when(activeObjects.executeInTransaction(any(TransactionCallback.class))).thenReturn(true);

        Collection<Event> firstPage = Lists.newArrayList(mockEvent("2", 2));
        when(events.iterator()).thenReturn(events);
        when(events.next()).thenReturn(firstPage);
        when(events.hasNext()).thenReturn(true, false);

        gitHubEventService.synchronize(repository, true, new String[] { }, false);

        verify(gitHubEventDAO).getLastSavePoint(eq(repository));
        verifyNoMoreInteractions(gitHubEventDAO);
        verifyNoMoreInteractions(messagingService);
    }

    @Test
    public void testProcessOneEventButDarkFeatureSetSoNoMoreMessages()
    {

        when(gitHubEventDAO.getLastSavePoint(repository)).thenReturn(savePointEvent);
        when(activeObjects.executeInTransaction(any(TransactionCallback.class))).thenReturn(false, true);
        when(syncDisabledHelper.isGitHubUsePullRequestListDisabled()).thenReturn(true);

        Collection<Event> firstPage = Lists.newArrayList(mockEvent("2", 2));
        when(events.iterator()).thenReturn(events);
        when(events.next()).thenReturn(firstPage);
        when(events.hasNext()).thenReturn(true, false);

        gitHubEventService.synchronize(repository, true, new String[] { }, false);

        verifyNoMoreInteractions(messagingService);
    }

    @Test
    public void testProcessOneEventDarkFeatureOff()
    {
        when(gitHubEventDAO.getByGitHubId(repository, "2")).thenReturn(newSavePointEvent);

        when(gitHubEventDAO.getLastSavePoint(repository)).thenReturn(savePointEvent);
        when(activeObjects.executeInTransaction(any(TransactionCallback.class))).thenReturn(false, true);
        when(syncDisabledHelper.isGitHubUsePullRequestListDisabled()).thenReturn(false);

        Collection<Event> firstPage = Lists.newArrayList(mockEvent("2", 2));
        when(events.iterator()).thenReturn(events);
        when(events.next()).thenReturn(firstPage);
        when(events.hasNext()).thenReturn(true, false);

        gitHubEventService.synchronize(repository, true, new String[] { }, false);

        verify(gitHubEventDAO).markAsSavePoint(newSavePointEvent);
    }

    @Test
    public void testProcessOneEvent()
    {
        when(gitHubEventDAO.getLastSavePoint(repository)).thenReturn(savePointEvent);
        when(activeObjects.executeInTransaction(any(TransactionCallback.class))).thenReturn(false, true);
        when(syncDisabledHelper.isGitHubUsePullRequestListDisabled()).thenReturn(false);

        Collection<Event> firstPage = Lists.newArrayList(mockEvent("2", 2));
        when(events.iterator()).thenReturn(events);
        when(events.next()).thenReturn(firstPage);
        when(events.hasNext()).thenReturn(true, false);

        gitHubEventService.synchronize(repository, true, new String[] { }, false);

        verify(gitHubEventDAO).getLastSavePoint(eq(repository));
    }

    @Test
    public void testProcessTwoPagesOfEvents()
    {

        when(gitHubEventDAO.getLastSavePoint(repository)).thenReturn(savePointEvent);
        when(activeObjects.executeInTransaction(any(TransactionCallback.class))).thenReturn(false, true);
        when(syncDisabledHelper.isGitHubUsePullRequestListDisabled()).thenReturn(false);

        Collection<Event> firstPage = Lists.newArrayList(mockEvent("2", 2));
        Collection<Event> secondPage = Lists.newArrayList(mockEvent("3", 3));
        when(events.iterator()).thenReturn(events);
        when(events.next()).thenReturn(firstPage, secondPage);
        when(events.hasNext()).thenReturn(true, true, false);

        gitHubEventService.synchronize(repository, true, new String[] { }, false);

        verify(activeObjects, times(2)).executeInTransaction(any(TransactionCallback.class));
    }

    private Event mockEvent(String id, long date)
    {
        Event event = mock(Event.class);
        when(event.getCreatedAt()).thenReturn(new Date(date));
        when(event.getId()).thenReturn(id);
        return event;
    }
}
