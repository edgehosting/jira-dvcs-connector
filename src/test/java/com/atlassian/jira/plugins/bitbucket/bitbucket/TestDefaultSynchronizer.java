package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.concurrent.Executors;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.common.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.common.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.mapper.impl.DefaultSynchronizer;
import com.atlassian.templaterenderer.TemplateRenderer;

/**
 * Unit tests for {@link DefaultSynchronizer}
 */
public class TestDefaultSynchronizer
{
	@Mock
	private Bitbucket bitbucket;
	@Mock
	private Changeset changeset;
	@Mock
	private TemplateRenderer templateRenderer;
	@Mock
	private RepositoryManager repositoryManager;
	@Mock
	private SourceControlRepository repository;

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
		when(repositoryManager.getRepository(projectKey, repository.getUrl())).thenReturn(repository);
		when(bitbucket.getChangesets(repository)).thenReturn(Arrays.asList(changeset));
		when(changeset.getMessage()).thenReturn("PRJ-1 Message");

		RepositoryUri uri = RepositoryUri.parse(repository.getUrl());
		new DefaultSynchronizer(bitbucket, Executors.newSingleThreadExecutor(), templateRenderer, repositoryManager)
				.synchronize(projectKey, uri);
		verify(repositoryManager, times(1)).addChangeset("PRJ-1", changeset);

	}

}
