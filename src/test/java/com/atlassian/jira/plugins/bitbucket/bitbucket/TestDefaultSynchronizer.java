package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.mapper.BitbucketMapper;
import com.atlassian.jira.plugins.bitbucket.mapper.impl.DefaultSynchronizer;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.atlassian.util.concurrent.ThreadFactories;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.MockitoAnnotations.Mock;

import java.util.Arrays;
import java.util.concurrent.Executors;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultSynchronizer}
 */
public class TestDefaultSynchronizer
{
    @Mock
    private Bitbucket bitbucket;
    @Mock
    private BitbucketMapper bitbucketMapper;
    @Mock
    private BitbucketChangeset changeset;
    @Mock
    private TemplateRenderer templateRenderer;

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
        when(bitbucketMapper.getAuthentication(projectKey,repositoryUri)).thenReturn(BitbucketAuthentication.ANONYMOUS);
        when(bitbucket.getChangesets(BitbucketAuthentication.ANONYMOUS, "owner", "slug")).thenReturn(Arrays.asList(changeset));
        when(changeset.getMessage()).thenReturn("PRJ-1 Message");

        new DefaultSynchronizer(bitbucket, bitbucketMapper,
                Executors.newSingleThreadExecutor(), templateRenderer).synchronize(projectKey, repositoryUri);
        verify(bitbucketMapper, times(1)).addChangeset("PRJ-1", changeset);
    }
}
