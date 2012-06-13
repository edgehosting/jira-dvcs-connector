package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * @author Martin Skurla
 */
@RunWith(MockitoJUnitRunner.class)
public final class TestDefaultSynchronizer
{
    @Mock
    private Repository repositoryMock;

    @Mock
    private ChangesetService changesetServiceMock;

    @Captor
    private ArgumentCaptor<Changeset> savedChangesetCaptor;


    private Changeset changesetWithJIRAIssue    = new Changeset(123, "node", "message MES-123 text",       new Date());
    private Changeset changesetWithoutJIRAIssue = new Changeset(123, "node", "message without JIRA issue", new Date());

    //TODO if soft sync, bude sa volat lastCommitDate, inak nie???

    @Test
    public void softSynchronization_ShouldSaveOneChangeset() throws InterruptedException
    {
        Date lastCommitDate = new Date();

        when(repositoryMock.getLastCommitDate()).thenReturn(lastCommitDate);

        when(changesetServiceMock.getChangesetsFromDvcs(eq(repositoryMock), eq(lastCommitDate)))
                                 .thenReturn(Arrays.asList(changesetWithJIRAIssue, changesetWithoutJIRAIssue));

        SynchronisationOperation synchronisationOperation =
                new DefaultSynchronisationOperation(repositoryMock,
                                                    mock(RepositoryService.class),
                                                    changesetServiceMock,
                                                    true); // soft sync

        Synchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor());
        synchronizer.synchronize(repositoryMock, synchronisationOperation);

        waitUntilProgressEnds(synchronizer);

        verify(changesetServiceMock, times(1)).save(savedChangesetCaptor.capture());

        assertThat(savedChangesetCaptor.getValue().getIssueKey(), is("MES-123"));
    }

    private void waitUntilProgressEnds(Synchronizer synchronizer) throws InterruptedException
    {
        Progress progress = synchronizer.getProgress(repositoryMock);

        while (!progress.isFinished())
        {
            Thread.sleep(50);
        }
    }
}
