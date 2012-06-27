package com.atlassian.jira.plugins.dvcs.sync.impl;

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
import com.atlassian.jira.plugins.dvcs.smartcommits.CommitMessageParser;
import com.atlassian.jira.plugins.dvcs.smartcommits.SmartcommitsChangesetsProcessor;
import com.atlassian.jira.plugins.dvcs.smartcommits.SmartcommitsService;
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
	private SmartcommitsService smartcommitsServiceMock;

	@Mock
	private CommitMessageParser commitMessageParserMock;

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
		Date lastCommitDate = new Date();

		when(repositoryMock.getLastCommitDate()).thenReturn(lastCommitDate);

		when(changesetServiceMock.getChangesetsFromDvcs(eq(repositoryMock), eq(lastCommitDate))).thenReturn(
				Arrays.asList(changesetWithJIRAIssue, changesetWithoutJIRAIssue));

		SynchronisationOperation synchronisationOperation = new DefaultSynchronisationOperation(repositoryMock,
				mock(RepositoryService.class), changesetServiceMock, true); // soft
																			// sync

		Synchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadScheduledExecutor(),
				changesetsProcessorMock, changesetServiceMock, smartcommitsServiceMock, commitMessageParserMock);
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
