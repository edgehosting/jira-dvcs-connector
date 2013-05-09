package com.atlassian.jira.plugins.dvcs.sync.impl;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Executors;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.smartcommits.SmartcommitsChangesetsProcessor;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;

/**
 * @author Martin Skurla
 */
public final class TestDefaultSynchronizer
{
    @Mock
    private Repository repositoryMock;

    @Mock
    private ChangesetService changesetServiceMock;
    
    @Mock
    DvcsCommunicator communicatorMock;


    @Mock
    private SmartcommitsChangesetsProcessor changesetsProcessorMock;
    
    @Captor
    private ArgumentCaptor<Changeset> savedChangesetCaptor;

    @Captor
    private ArgumentCaptor<Set<String>> extractedIssuesCaptor;
    
    private final Changeset changesetWithJIRAIssue = new Changeset(123, "node", "message MES-123 text", new Date());
    private final Changeset changesetWithoutJIRAIssue = new Changeset(123, "node", "message without JIRA issue",
            new Date());

    @BeforeMethod
    private void initializeMocks()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void softSynchronization_ShouldSaveOneChangesetWithIssueKey() throws InterruptedException
    {
        when(changesetServiceMock.getChangesetsFromDvcs(eq(repositoryMock))).thenReturn(
                Arrays.asList(changesetWithJIRAIssue, changesetWithoutJIRAIssue));

        SynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(communicatorMock, repositoryMock,
                mock(RepositoryService.class), changesetServiceMock, true); // soft sync

        Synchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor(), changesetsProcessorMock);
        synchronizer.synchronize(repositoryMock, synchronisationOperation);

        waitUntilProgressEnds(synchronizer);

        verify(changesetServiceMock, times(2)).create(savedChangesetCaptor.capture(), extractedIssuesCaptor.capture());
        
        // one changeset is saved with issue key, another without
        assertThat(extractedIssuesCaptor.getAllValues().get(0).contains("MES-123")).isTrue();
        assertThat(extractedIssuesCaptor.getAllValues().get(1).isEmpty()).isTrue();
    }
    
    @Test
    public void gettingDiffsOnlyForChangesetsWithIssueKeys() throws InterruptedException
    {
        when(changesetServiceMock.getChangesetsFromDvcs(eq(repositoryMock))).thenReturn(
                Arrays.asList(changesetWithJIRAIssue, changesetWithoutJIRAIssue));

        when(changesetServiceMock.getDetailChangesetFromDvcs(eq(repositoryMock), any(Changeset.class))).then(
                new Answer<Changeset>()
                {
                    @Override
                    public Changeset answer(InvocationOnMock invocation) throws Throwable {
                      Object[] args = invocation.getArguments();
                      Changeset argChangeset =  (Changeset) args[1];
                      Changeset changeset = new Changeset(
                                                      argChangeset.getRepositoryId(),
                                                      argChangeset.getNode(),
                                                      argChangeset.getRawAuthor(),
                                                      argChangeset.getAuthor(),
                                                      argChangeset.getDate(),
                                                      argChangeset.getRawNode(),
                                                      argChangeset.getBranch(),
                                                      argChangeset.getMessage(),
                                                      argChangeset.getParents(),
                                                      Arrays.asList(new ChangesetFile(ChangesetFileAction.ADDED,"file",1,0)),
                                                      1,
                                                      argChangeset.getAuthorEmail());
                      
                      return changeset;
                    }
                  });
        
        SynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(communicatorMock, repositoryMock,
                mock(RepositoryService.class), changesetServiceMock, true); // soft sync

        Synchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor(), changesetsProcessorMock);
        synchronizer.synchronize(repositoryMock, synchronisationOperation);

        waitUntilProgressEnds(synchronizer);

        verify(changesetServiceMock, times(2)).create(savedChangesetCaptor.capture(), anySetOf(String.class));
        
        // one changeset has file details, another not
        assertThat(savedChangesetCaptor.getAllValues().get(0).getFiles()).isNotEmpty();
        assertThat(savedChangesetCaptor.getAllValues().get(1).getFiles()).isEmpty();
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
