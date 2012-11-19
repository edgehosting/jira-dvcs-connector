package com.atlassian.jira.plugins.dvcs.sync.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.Executors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
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
@RunWith(MockitoJUnitRunner.class)
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

	private final Changeset changesetWithJIRAIssue = new Changeset(123, "node", "message MES-123 text", new Date());
	private final Changeset changesetWithoutJIRAIssue = new Changeset(123, "node", "message without JIRA issue",
			new Date());

	@Test
	public void softSynchronization_ShouldSaveOneChangeset() throws InterruptedException
	{
		when(changesetServiceMock.getChangesetsFromDvcs(eq(repositoryMock))).thenReturn(
				Arrays.asList(changesetWithJIRAIssue, changesetWithoutJIRAIssue));

		SynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(communicatorMock, repositoryMock,
                mock(RepositoryService.class), changesetServiceMock, true); // soft sync

		Synchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor(), changesetsProcessorMock);
		synchronizer.synchronize(repositoryMock, synchronisationOperation);

		waitUntilProgressEnds(synchronizer);
       	
		verify(changesetServiceMock, times(2)).save(savedChangesetCaptor.capture());
        
		assertThat(savedChangesetCaptor.getAllValues().get(0).getIssueKey(), is("MES-123"));
		assertThat(savedChangesetCaptor.getAllValues().get(1).getIssueKey(), is("NON_EXISTING-0"));
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
