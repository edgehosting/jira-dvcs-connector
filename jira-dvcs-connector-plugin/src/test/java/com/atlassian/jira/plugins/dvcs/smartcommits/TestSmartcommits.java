package com.atlassian.jira.plugins.dvcs.smartcommits;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.EnumSet;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.plugins.dvcs.sync.impl.DefaultSynchronisationOperation;

/**
 * @author Martin Skurla
 */
// DISABLED: sync operation is not used anymore
public final class TestSmartcommits
{
    @Mock
    private Repository repositoryMock;

    @Mock
    private ChangesetService changesetServiceMock;

    @Mock
    private BranchService branchServiceMock;

    @Mock
    private SmartcommitsChangesetsProcessor changesetsProcessorMock;

    @Mock
    DvcsCommunicator communicatorMock;

    @Captor
	private ArgumentCaptor<Changeset> savedChangesetCaptor;

    private Changeset changesetWithJIRAIssue()
    {
        return new Changeset(123, "node", "message MES-123 text", new Date());
    }

    private Changeset changesetWithoutJIRAIssue()
    {
        return new Changeset(123, "node", "message without JIRA issue", new Date());
    }


    @BeforeMethod
    private void initializeMocks()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test(enabled = false)
    public void softSynchronization_ShouldMarkSmartcommit() throws InterruptedException
    {
        when(repositoryMock.isSmartcommitsEnabled()).thenReturn(Boolean.TRUE);

		when(changesetServiceMock.getChangesetsFromDvcs(eq(repositoryMock))).thenReturn(
				Arrays.asList(changesetWithJIRAIssue(), changesetWithoutJIRAIssue()));

		SynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(
                communicatorMock,
                repositoryMock,
                mock(RepositoryService.class),
                changesetServiceMock,
                branchServiceMock,
                EnumSet.of(SynchronizationFlag.SOFT_SYNC, SynchronizationFlag.SYNC_CHANGESETS, SynchronizationFlag.SYNC_PULL_REQUESTS)); // soft sync

        //Synchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor(), changesetsProcessorMock);
       // synchronizer.synchronize(repositoryMock, synchronisationOperation, changesetServiceMock);

        //waitUntilProgressEnds(synchronizer);

        verify(changesetsProcessorMock).startProcess(new DefaultProgress(), repositoryMock, changesetServiceMock);
        verify(changesetServiceMock, times(2)).create(savedChangesetCaptor.capture(), anySetOf(String.class));

        assertThat(savedChangesetCaptor.getAllValues().get(0).isSmartcommitAvaliable()).isTrue();
        assertThat(savedChangesetCaptor.getAllValues().get(1).isSmartcommitAvaliable()).isFalse();
    }


    @Test(enabled = false)
    public void softSynchronization_ShouldnotMarkSmartcommit() throws InterruptedException
    {
        when(repositoryMock.isSmartcommitsEnabled()).thenReturn(Boolean.FALSE);

        changesetServiceMock = mock(ChangesetService.class);
		when(changesetServiceMock.getChangesetsFromDvcs(eq(repositoryMock))).thenReturn(
				Arrays.asList(changesetWithJIRAIssue(), changesetWithoutJIRAIssue()));

		SynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(
                communicatorMock,
                repositoryMock,
                mock(RepositoryService.class),
                changesetServiceMock,
                branchServiceMock,
                EnumSet.of(SynchronizationFlag.SOFT_SYNC, SynchronizationFlag.SYNC_CHANGESETS, SynchronizationFlag.SYNC_PULL_REQUESTS)); // soft sync

        //Synchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor(), changesetsProcessorMock);
        //synchronizer.synchronize(repositoryMock, synchronisationOperation, changesetServiceMock);

        //waitUntilProgressEnds(synchronizer);

        verify(changesetsProcessorMock).startProcess(new DefaultProgress(), repositoryMock, changesetServiceMock);
        verify(changesetServiceMock, times(2)).create(savedChangesetCaptor.capture(), anySetOf(String.class));

        assertThat(savedChangesetCaptor.getAllValues().get(0).isSmartcommitAvaliable()).isNull();
        assertThat(savedChangesetCaptor.getAllValues().get(1).isSmartcommitAvaliable()).isNull();
    }

    private void waitUntilProgressEnds(Synchronizer synchronizer) throws InterruptedException
    {
        Progress progress = synchronizer.getProgress(repositoryMock.getId());

        while (!progress.isFinished())
        {
            Thread.sleep(50);
        }
    }
}
