package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.BitbucketChangesetIssueMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.activeobjects.BitbucketRepositoryProjectMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.BasicAuthentication;
import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.DefaultBitbucketMapper;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.util.List;
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
    BitbucketRepositoryProjectMapping repositoryProjectMapping;
    @Mock
    BitbucketChangesetIssueMapping changesetIssueMapping;
    @Mock
    BitbucketRepository repository;
    @Mock
    BitbucketChangeset changeset;
    @Mock
    Encryptor encryptor;

    @Before
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(repositoryProjectMapping.getRepositoryOwner()).thenReturn("owner");
        when(repositoryProjectMapping.getRepositorySlug()).thenReturn("slug");

        when(activeObjects.find(BitbucketRepositoryProjectMapping.class, "projectKey = ?", "JST")).thenReturn(
                new BitbucketRepositoryProjectMapping[]{repositoryProjectMapping});

        when(repository.getOwner()).thenReturn("owner");
        when(repository.getSlug()).thenReturn("slug");
        when(repository.getRepositoryUrl()).thenReturn("https://bitbucket.org/owner/slug");
        when(changeset.getRepositoryOwner()).thenReturn("owner");
        when(changeset.getRepositorySlug()).thenReturn("slug");
        when(changeset.getNode()).thenReturn("1");
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
        when(encryptor.encrypt(anyString(), anyString(), anyString())).thenAnswer(
                new Answer<String>()
                {
                    public String answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return StringUtils.reverse(String.valueOf(invocationOnMock.getArguments()[0]));
                    }
                }
        );
    }

    @Test
    public void testGetRepositoryIsLazy()
    {
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).getRepositories("JST");
        verify(bitbucket, never()).getRepository(any(BitbucketAuthentication.class), anyString(), anyString());
    }

    @Test
    public void testAddAnonymousRepositoryCreatesValidMap()
    {
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).addRepository("JST", repository, null, null);
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
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).addRepository("JST", repository, "user", "pass");
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
                                map.containsKey("password");
                    }
                }));
    }

    @Test
    public void testPasswordNotStoredInPlainText()
    {
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).addRepository("JST", repository, "user", "pass");
        verify(activeObjects, times(1)).create(eq(BitbucketRepositoryProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return !map.get("password").equals("pass");
                    }
                }));
        verify(encryptor, times(1)).encrypt("pass", "JST", "https://bitbucket.org/owner/slug");
    }

    @Test
    public void testRemoveRepositoryAlsoRemovesIssues()
    {
        when(activeObjects.find(BitbucketRepositoryProjectMapping.class,
                "projectKey = ? and repositoryOwner = ? and repositorySlug = ?",
                "JST", "owner", "slug")).thenReturn(new BitbucketRepositoryProjectMapping[]{repositoryProjectMapping}
        );
        when(activeObjects.find(BitbucketChangesetIssueMapping.class,
                "projectKey = ? and repositoryOwner = ? and repositorySlug = ?",
                "JST", "owner", "slug")).thenReturn(new BitbucketChangesetIssueMapping[]{changesetIssueMapping}
        );
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).removeRepository("JST", repository);
        verify(activeObjects, times(1)).delete(repositoryProjectMapping);
        verify(activeObjects, times(1)).delete(changesetIssueMapping);
    }

    @Test
    public void testGetChangesets()
    {
        when(activeObjects.find(BitbucketRepositoryProjectMapping.class,
                "projectKey = ? and repositoryOwner = ? and repositorySlug = ?",
                "JST", "owner", "slug")).thenReturn(new BitbucketRepositoryProjectMapping[]{repositoryProjectMapping}
        );
        when(activeObjects.find(BitbucketChangesetIssueMapping.class,
                "issueId = ?", "JST-1")).thenReturn(new BitbucketChangesetIssueMapping[]{changesetIssueMapping}
        );
        when(changesetIssueMapping.getNode()).thenReturn("1");
        when(changesetIssueMapping.getRepositoryOwner()).thenReturn("owner");
        when(changesetIssueMapping.getRepositorySlug()).thenReturn("slug");
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).getChangesets("JST-1");
        verify(bitbucket, times(1)).getChangeset(argThat(new ArgumentMatcher<BitbucketAuthentication>()
        {
            @Override
            public boolean matches(Object o)
            {
                return o == BitbucketAuthentication.ANONYMOUS;
            }
        }), eq("owner"), eq("slug"), eq("1"));
    }

    @Test
    public void testGetChangesetsOnAuthenticatedRepository()
    {
        when(activeObjects.find(BitbucketRepositoryProjectMapping.class,
                "projectKey = ? and repositoryOwner = ? and repositorySlug = ?",
                "JST", "owner", "slug")).thenReturn(new BitbucketRepositoryProjectMapping[]{repositoryProjectMapping}
        );
        when(activeObjects.find(BitbucketChangesetIssueMapping.class,
                "issueId = ?", "JST-1")).thenReturn(new BitbucketChangesetIssueMapping[]{changesetIssueMapping}
        );
        when(repositoryProjectMapping.getUsername()).thenReturn("user");
        when(repositoryProjectMapping.getPassword()).thenReturn("pass");
        when(changesetIssueMapping.getNode()).thenReturn("1");
        when(changesetIssueMapping.getRepositoryOwner()).thenReturn("owner");
        when(changesetIssueMapping.getRepositorySlug()).thenReturn("slug");
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).getChangesets("JST-1");
        verify(bitbucket, times(1)).getChangeset(argThat(new ArgumentMatcher<BitbucketAuthentication>()
        {
            @Override
            public boolean matches(Object o)
            {
                BasicAuthentication auth = (BasicAuthentication) o;
                return auth.getUsername().equals("user") && auth.getPassword().equals("pass");
            }
        }), eq("owner"), eq("slug"), eq("1"));
    }

    @Test
    public void testAddChangeset()
    {
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).addChangeset("JST-1", changeset);
        verify(activeObjects, times(1)).create(eq(BitbucketChangesetIssueMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("repositoryOwner").equals("owner") &&
                                map.get("repositorySlug").equals("slug") &&
                                map.get("projectKey").equals("JST") &&
                                map.get("node").equals("1") &&
                                map.get("issueId").equals("JST-1");
                    }
                }));
    }
}
