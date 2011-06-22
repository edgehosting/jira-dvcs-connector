package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.BitbucketRepositoryProjectMapping;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultBitbucketMapper}
 */
public class TestDefaultBitbucketMapper
{
    @Mock
    ActiveObjects activeObjects;
    @Mock
    Bitbucket bitbucket;
    @Mock
    BitbucketRepositoryProjectMapping mapping;
    @Mock
    BitbucketRepository repository;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(mapping.getRepositoryOwner()).thenReturn("owner");
        when(mapping.getRepositorySlug()).thenReturn("slug");

        when(activeObjects.find(BitbucketRepositoryProjectMapping.class, "projectKey = ?", "JST")).thenReturn(
                new BitbucketRepositoryProjectMapping[]{mapping});
    }

    @Test
    public void testGetRepositoryIsLazy()
    {
        new DefaultBitbucketMapper(activeObjects, bitbucket).getRepositories("JST");
        verify(bitbucket, never()).getRepository(any(BitbucketAuthentication.class), anyString(), anyString());
    }

    @Test
    public void testAddAnonymousRepositoryCreatesValidMap()
    {
        when(repository.getOwner()).thenReturn("owner");
        when(repository.getSlug()).thenReturn("slug");
        new DefaultBitbucketMapper(activeObjects, bitbucket).addRepository("JST", repository, null, null);
        verify(repository, times(1)).getOwner();
        verify(repository, times(1)).getSlug();
        verify(activeObjects, times(1)).create()

    }
}
