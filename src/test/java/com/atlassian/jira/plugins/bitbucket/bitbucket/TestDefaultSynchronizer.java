package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Arrays;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.bitbucket.DefaultSynchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketSynchronisation;

/**
 * Unit tests for {@link DefaultSynchronizer}
 */
public class TestDefaultSynchronizer
{
	@Mock
	private BitbucketCommunicator bitbucket;
	@Mock
	private Changeset changeset;
	@Mock
	private RepositoryManager repositoryManager;
	@Mock
	private SourceControlRepository repository;
	@Mock
	private ProgressWriter progressProvider;

	@Before
	public void setup()
	{
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testSynchronizeAddsSingleMapping() throws InterruptedException
	{
		when(repository.getUrl()).thenReturn("https://bitbucket.org/owner/slug");
		when(repository.getProjectKey()).thenReturn("PRJ");
		SynchronizationKey key = new SynchronizationKey(repository);
		BitbucketSynchronisation synchronisation = new BitbucketSynchronisation(key, repositoryManager, bitbucket, progressProvider);
		when(repositoryManager.getSynchronisationOperation(any(SynchronizationKey.class), any(ProgressWriter.class))).thenReturn(synchronisation);
		when(bitbucket.getChangesets(repository)).thenReturn(Arrays.asList(changeset));
		when(changeset.getMessage()).thenReturn("PRJ-1 Message");

		DefaultSynchronizer synchronizer = new DefaultSynchronizer(Executors.newSingleThreadExecutor(), repositoryManager);
		assertNull(synchronizer.getProgress(repository));
		synchronizer.synchronize(repository);
		assertNotNull(synchronizer.getProgress(repository));
		
		Progress progress = synchronizer.getProgress(repository);
		while (!progress.isFinished())
		{
			Thread.sleep(10);
		}
		verify(repositoryManager, times(1)).addChangeset(repository, "PRJ-1", changeset);
	}

}
