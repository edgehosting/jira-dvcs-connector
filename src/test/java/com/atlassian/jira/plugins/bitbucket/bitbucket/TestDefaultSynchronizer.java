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
import com.atlassian.jira.plugins.bitbucket.mapper.RepositoryPersister;
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
    private RepositoryPersister repositoryPersister;
    @Mock
    private Changeset changeset;
    @Mock
    private TemplateRenderer templateRenderer;
    @Mock
    private AuthenticationFactory authenticationFactory;
    @Mock
    private RepositoryManager repositoryManager; 
    
    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testSynchronizeAddsSingleMapping()
    {
        String projectKey = "PRJ";
        RepositoryUri repositoryUri = RepositoryUri.parse("owner/slug");
		SourceControlRepository repository = repositoryManager.getRepository(projectKey,repositoryUri.getRepositoryUrl());
        when(authenticationFactory.getAuthentication(repository)).thenReturn(Authentication.ANONYMOUS);
        when(bitbucket.getChangesets(Authentication.ANONYMOUS, "owner", "slug")).thenReturn(Arrays.asList(changeset));
        when(changeset.getMessage()).thenReturn("PRJ-1 Message");

        new DefaultSynchronizer(bitbucket, Executors.newSingleThreadExecutor(), templateRenderer, authenticationFactory, repositoryManager).synchronize(projectKey, repositoryUri);
        verify(repositoryManager, times(1)).addChangeset("PRJ-1", changeset);
    }
}
