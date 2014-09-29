package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.dao.GitHubEventDAO;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessorAggregator;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.EventPayload;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests the transaction callback returns the right values and does the appropriate work
 */
public class SyncTransactionCallbackTest
{
    private static final long LAST_PROCESSED_MILLIS = 3;
    private static final long DEFAULT_PROCESSED_MILLIS = 4;
    private static final String EVENT_ID = "123ABC";

    private final boolean isSoftSync = true;
    private final String[] synchronizationTags = new String[] { };
    private final Date lastProcessedEventDate = new Date(LAST_PROCESSED_MILLIS);

    private GitHubEventServiceImpl.SyncTransactionCallback syncTransactionCallback;

    @Mock
    private GitHubEventMapping lastGitHubEventSavePoint;
    @Mock
    private GitHubEventMapping existingEvent;
    @Mock
    private Repository repository;
    @Mock
    private Set<String> processedEventIds;
    @Mock
    private GitHubEventDAO gitHubEventDAO;
    @Mock
    private GitHubEventProcessorAggregator<EventPayload> gitHubEventProcessorAggregator;

    private GitHubEventContextImpl context;

    private Event event;

    @BeforeMethod
    public void init()
    {
        MockitoAnnotations.initMocks(this);

        processedEventIds = new HashSet<String>();
        context = new GitHubEventContextImpl(null, null, repository, isSoftSync, synchronizationTags, true);

        when(lastGitHubEventSavePoint.getCreatedAt()).thenReturn(lastProcessedEventDate);
        event = new Event();
        event.setCreatedAt(new Date(DEFAULT_PROCESSED_MILLIS));
        event.setId(EVENT_ID);

        syncTransactionCallback = new GitHubEventServiceImpl.SyncTransactionCallback(processedEventIds,
                lastGitHubEventSavePoint, event, context, gitHubEventDAO, gitHubEventProcessorAggregator);
    }

    @Test
    public void testEventBeforeLastCheckpoint()
    {
        event.setCreatedAt(new Date(LAST_PROCESSED_MILLIS - 1));
        assertThat(syncTransactionCallback.doInTransaction(), is(true));
        verifyZeroInteractions(gitHubEventProcessorAggregator);
    }

    @Test
    public void testNullLastCheckpoint()
    {
        syncTransactionCallback = new GitHubEventServiceImpl.SyncTransactionCallback(processedEventIds,
                null, event, context, gitHubEventDAO, gitHubEventProcessorAggregator);
        assertThat(syncTransactionCallback.doInTransaction(), is(false));
    }

    @Test
    public void testEventAlreadyPersisted()
    {
        when(gitHubEventDAO.getByGitHubId(repository, event.getId())).thenReturn(existingEvent);
        assertThat(syncTransactionCallback.doInTransaction(), is(false));
        verifyZeroInteractions(gitHubEventProcessorAggregator);
    }

    @Test
    public void testEventAlreadyProcessedButNotReturnedWhenRetrieved()
    {
        processedEventIds.add(EVENT_ID);
        assertThat(syncTransactionCallback.doInTransaction(), is(false));
        verifyZeroInteractions(gitHubEventProcessorAggregator);
    }

    @Test
    public void testRegularRun()
    {
        assertThat(syncTransactionCallback.doInTransaction(), is(false));
    }
}
