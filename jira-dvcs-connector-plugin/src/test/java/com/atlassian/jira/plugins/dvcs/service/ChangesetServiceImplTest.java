package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.beehive.ClusterLock;
import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.event.ChangesetCreatedEvent;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.google.common.collect.ImmutableSet;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
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

    private ChangesetServiceImpl changesetService;

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(clusterLockServiceFactory.getClusterLockService()).thenReturn(clusterLockService);
        when(clusterLockService.getLockForName(anyString())).thenReturn(clusterLock);

        changesetService = new ChangesetServiceImpl(changesetDao, clusterLockServiceFactory);
        ReflectionTestUtils.setField(changesetService, "threadEvents", threadEvents);
    }

    @Test
    public void createShouldBroadcastChangesetCreatedEventForNewChangesets() throws Exception
    {
        // int repositoryId, String node, String message, Date timestam
        Changeset changeset = new Changeset(1, "d11320d1e9321f4ea96f9b46ecae20027a85dc7b", "DEV-1: file changed", new Date());
        when(changesetDao.createOrAssociate(changeset, ImmutableSet.of("DEV-1"))).thenReturn(true);

        changesetService.create(changeset, ImmutableSet.of("DEV-1"));

        ArgumentCaptor<Object> eventCaptor = ArgumentCaptor.forClass(Object.class);
        verify(threadEvents).broadcast(eventCaptor.capture());

        assertThat(eventCaptor.getValue(), instanceOf(ChangesetCreatedEvent.class));
        ChangesetCreatedEvent event = (ChangesetCreatedEvent) eventCaptor.getValue();
        assertThat(event.getChangeset(), is(changeset));
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
