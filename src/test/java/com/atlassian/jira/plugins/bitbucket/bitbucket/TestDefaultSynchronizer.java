package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.atlassian.jira.plugins.bitbucket.DefaultSynchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketSynchronisation;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.base.Function;

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
	private TemplateRenderer templateRenderer;
	@Mock
	private RepositoryManager repositoryManager;
	@Mock
	private SourceControlRepository repository;
	@Mock
	private Function<SynchronizationKey, Progress> progressProvider;

	@Before
	public void setup()
	{
		MockitoAnnotations.initMocks(this);
	}

	@Test
	public void testSynchronizeAddsSingleMapping()
	{
		String projectKey = "PRJ";
		when(repository.getUrl()).thenReturn("https://bitbucket.org/owner/slug");
		RepositoryUri uri = RepositoryUri.parse(repository.getUrl());

		when(repositoryManager.getRepository(projectKey, repository.getUrl())).thenReturn(repository);
		SynchronizationKey key = new SynchronizationKey("PRJ", uri.getRepositoryUrl());
		BitbucketSynchronisation synchronisation = new BitbucketSynchronisation(key, repositoryManager, bitbucket, progressProvider);
		when(repositoryManager.getSynchronisationOperation(any(SynchronizationKey.class), any(Function.class))).thenReturn(synchronisation);
		when(bitbucket.getChangesets(repository)).thenReturn(Arrays.asList(changeset));
		when(changeset.getMessage()).thenReturn("PRJ-1 Message");

		new DefaultSynchronizer(Executors.newSingleThreadExecutor(), templateRenderer, repositoryManager)
				.synchronize(projectKey, uri.getRepositoryUrl());
		verify(repositoryManager, times(1)).addChangeset("PRJ-1", changeset);
	}

}
