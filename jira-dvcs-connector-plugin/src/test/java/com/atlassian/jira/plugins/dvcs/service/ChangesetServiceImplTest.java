package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.beehive.ClusterLock;
import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.event.ChangesetCreatedEvent;
import com.atlassian.jira.plugins.dvcs.event.DevSummaryChangedEvent;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.google.common.collect.ImmutableSet;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


public class ChangesetServiceImplTest
{
    @Mock
    private ThreadEvents threadEvents;

    @Mock
    private ChangesetDao changesetDao;

    @Mock
    private ClusterLockService clusterLockService;

    @Mock
    private ClusterLock clusterLock;

    @Mock
    private ClusterLockServiceFactory clusterLockServiceFactory;

    @Mock
    private RepositoryDao repositoryDao;

    private ChangesetServiceImpl changesetService;

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(clusterLockServiceFactory.getClusterLockService()).thenReturn(clusterLockService);
        when(clusterLockService.getLockForName(anyString())).thenReturn(clusterLock);

        changesetService = new ChangesetServiceImpl(changesetDao, clusterLockServiceFactory);
        ReflectionTestUtils.setField(changesetService, "repositoryDao", repositoryDao);
        ReflectionTestUtils.setField(changesetService, "threadEvents", threadEvents);
    }

    @Test
    public void createShouldBroadcastChangesetCreatedEventForNewChangesets() throws Exception
    {
        // int repositoryId, String node, String message, Date timestam
        Changeset changeset = new Changeset(1, "d11320d1e9321f4ea96f9b46ecae20027a85dc7b", "DEV-1: file changed", new Date());
        final String issueKey = "DEV-1";
        when(changesetDao.createOrAssociate(changeset, ImmutableSet.of(issueKey))).thenReturn(true);
        when(repositoryDao.get(anyInt())).thenReturn(new Repository());

        changesetService.create(changeset, ImmutableSet.of(issueKey));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(threadEvents, times(2)).broadcast(eventCaptor.capture());
        verify(repositoryDao).get(anyInt());

        assertThat(eventCaptor.getAllValues().get(0), instanceOf(ChangesetCreatedEvent.class));
        ChangesetCreatedEvent event = (ChangesetCreatedEvent) eventCaptor.getAllValues().get(0);
        assertThat(event.getChangeset(), is(changeset));

        assertThat(eventCaptor.getAllValues().get(1), instanceOf(DevSummaryChangedEvent.class));
        DevSummaryChangedEvent devSummaryChangedEvent = (DevSummaryChangedEvent) eventCaptor.getAllValues().get(1);
        assertThat(event.getIssueKeys(), contains(new String[] { issueKey }));
    }

    @Test
    public void createShouldNotBroadcastChangesetCreatedEventForExistingChangesets() throws Exception
    {
        // int repositoryId, String node, String message, Date timestam
        Changeset changeset = new Changeset(1, "d11320d1e9321f4ea96f9b46ecae20027a85dc7b", "DEV-1: file changed", new Date());
        when(changesetDao.createOrAssociate(changeset, ImmutableSet.of("DEV-1"))).thenReturn(false);

        changesetService.create(changeset, ImmutableSet.of("DEV-1"));
        verify(threadEvents, never()).broadcast(anyObject());
    }
}
