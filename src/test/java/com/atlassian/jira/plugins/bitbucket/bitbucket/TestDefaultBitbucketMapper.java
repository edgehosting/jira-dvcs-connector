package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.BitbucketRepositoryProjectMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucketMapper;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.Map;

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

        when(repository.getOwner()).thenReturn("owner");
        when(repository.getSlug()).thenReturn("slug");
        //noinspection unchecked
        when(activeObjects.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(
                new Answer<Object>()
                {
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
                    }
                }
        );
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
        new DefaultBitbucketMapper(activeObjects, bitbucket).addRepository("JST", repository, null, null);
        verify(repository, times(1)).getOwner();
        verify(repository, times(1)).getSlug();
        verify(activeObjects, times(1)).create(eq(BitbucketRepositoryProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("repositoryOwner").equals("owner") &&
                                map.get("repositorySlug").equals("slug") &&
                                map.get("projectKey").equals("JST") &&
                                !map.containsKey("username") && !map.containsKey("password");
                    }
                }));
    }

    @Test
    public void testAddAuthentictedRepositoryCreatesValidMap()
    {
        new DefaultBitbucketMapper(activeObjects, bitbucket).addRepository("JST", repository, "user", "pass");
        verify(repository, times(1)).getOwner();
        verify(repository, times(1)).getSlug();
        verify(activeObjects, times(1)).create(eq(BitbucketRepositoryProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("repositoryOwner").equals("owner") &&
                                map.get("repositorySlug").equals("slug") &&
                                map.get("projectKey").equals("JST") &&
                                map.get("username").equals("user") &&
                                map.get("password").equals("pass");
                    }
                }));
    }
}
