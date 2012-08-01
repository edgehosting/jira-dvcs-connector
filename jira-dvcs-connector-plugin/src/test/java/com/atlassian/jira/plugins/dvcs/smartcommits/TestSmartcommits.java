package com.atlassian.jira.plugins.dvcs.smartcommits;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
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
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.jira.plugins.dvcs.sync.impl.DefaultSynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.sync.impl.DefaultSynchronizer;

/**
 * @author Martin Skurla
 */
@RunWith(MockitoJUnitRunner.class)
public final class TestSmartcommits
{
	@Mock
	private Repository repositoryMock;

	@Mock
	private ChangesetService changesetServiceMock;


	@Mock
	private SmartcommitsChangesetsProcessor changesetsProcessorMock;
    
	@Captor
	private ArgumentCaptor<Changeset> savedChangesetCaptor;

	private final Changeset changesetWithJIRAIssue = new Changeset(123, "node", "message MES-123 text", new Date());
	private final Changeset changesetWithoutJIRAIssue = new Changeset(123, "node", "message without JIRA issue",
			new Date());

	/**
	 * @throws InterruptedException
	 */
	/**
	 * @throws InterruptedException
	 */
	@Test
	public void softSynchronization_ShouldMarkSmartcommit() throws InterruptedException
	{
		Date lastCommitDate = new Date();

		when(repositoryMock.getLastCommitDate()).thenReturn(lastCommitDate);
		
		when(repositoryMock.isSmartcommitsEnabled()).thenReturn(Boolean.TRUE);

		when(changesetServiceMock.getChangesetsFromDvcs(eq(repositoryMock), eq(lastCommitDate))).thenReturn(
				Arrays.asList(changesetWithJIRAIssue, changesetWithoutJIRAIssue));

		SynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(repositoryMock,
				mock(RepositoryService.class), changesetServiceMock, true); // soft sync

		Synchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor(), changesetsProcessorMock);
		synchronizer.synchronize(repositoryMock, synchronisationOperation);

		waitUntilProgressEnds(synchronizer);
       
		
		verify(changesetsProcessorMock).startProcess();
		verify(changesetServiceMock, times(1)).save(savedChangesetCaptor.capture());
		
		assertThat(savedChangesetCaptor.getValue().isSmartcommitAvaliable(), is(true));
	}
    

	@Test
	public void softSynchronization_ShouldnotMarkSmartcommit() throws InterruptedException
	{
		Date lastCommitDate = new Date();

		when(repositoryMock.getLastCommitDate()).thenReturn(lastCommitDate);
		
		when(repositoryMock.isSmartcommitsEnabled()).thenReturn(Boolean.FALSE);

		when(changesetServiceMock.getChangesetsFromDvcs(eq(repositoryMock), eq(lastCommitDate))).thenReturn(
				Arrays.asList(changesetWithJIRAIssue, changesetWithoutJIRAIssue));

		SynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(repositoryMock,
				mock(RepositoryService.class), changesetServiceMock, true); // soft sync

		Synchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor(), changesetsProcessorMock);
		synchronizer.synchronize(repositoryMock, synchronisationOperation);

		waitUntilProgressEnds(synchronizer);
       
		
		verify(changesetsProcessorMock).startProcess();
		verify(changesetServiceMock, times(1)).save(savedChangesetCaptor.capture());
		
		assertThat(savedChangesetCaptor.getValue().isSmartcommitAvaliable(), is((Boolean)null));
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