package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.mapper.activeobjects.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.mapper.activeobjects.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.impl.BasicAuthentication;
import com.atlassian.jira.plugins.bitbucket.mapper.Encryptor;
import com.atlassian.jira.plugins.bitbucket.mapper.impl.DefaultBitbucketMapper;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.apache.commons.lang.StringUtils;
import org.junit.After;
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
    ProjectMapping projectMapping;
    @Mock
    IssueMapping issueMapping;
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
        when(projectMapping.getRepositoryUri()).thenReturn("owner/slug/default");

        when(activeObjects.find(ProjectMapping.class, "project_key = ?", "JST")).thenReturn(
                new ProjectMapping[]{projectMapping});

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
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).
                addRepository("JST", RepositoryUri.parse("owner/slug/default"), null, null);
        verify(activeObjects, times(1)).create(eq(ProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("repository_uri").equals("owner/slug/default") &&
                                map.get("project_key").equals("JST") &&
                                !map.containsKey("username") && !map.containsKey("password");
                    }
                }));
    }

    @Test
    public void testAddAuthentictedRepositoryCreatesValidMap()
    {
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).
                addRepository("JST", RepositoryUri.parse("owner/slug/default"), "user", "pass");
        verify(activeObjects, times(1)).create(eq(ProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("repository_uri").equals("owner/slug/default") &&
                                map.get("project_key").equals("JST") &&
                                map.get("username").equals("user") &&
                                map.containsKey("password");
                    }
                }));
    }

    @Test
    public void testPasswordNotStoredInPlainText()
    {
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).
                addRepository("JST", RepositoryUri.parse("owner/slug/default"), "user", "pass");
        verify(activeObjects, times(1)).create(eq(ProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return !map.get("password").equals("pass");
                    }
                }));
        verify(encryptor, times(1)).encrypt("pass", "JST", "https://bitbucket.org/owner/slug/default");
    }

    @Test
    public void testRemoveRepositoryAlsoRemovesIssues()
    {
        when(activeObjects.find(ProjectMapping.class,
                "project_key = ? and repository_uri = ?",
                "JST", "owner/slug/default")).thenReturn(new ProjectMapping[]{projectMapping}
        );
        when(activeObjects.find(IssueMapping.class,
                "project_key = ? and repository_uri = ?",
                "JST", "owner/slug/default")).thenReturn(new IssueMapping[]{issueMapping}
        );
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).removeRepository("JST",
                RepositoryUri.parse("owner/slug/default"));
        verify(activeObjects, times(1)).find(ProjectMapping.class,
                "project_key = ? and repository_uri = ?", "JST", "owner/slug/default");
        verify(activeObjects, times(1)).find(IssueMapping.class,
                "project_key = ? and repository_uri = ?", "JST", "owner/slug/default");
        verify(activeObjects, times(1)).delete(projectMapping);
        verify(activeObjects, times(1)).delete(issueMapping);
    }

    @Test
    public void testGetChangesets()
    {
        when(activeObjects.find(ProjectMapping.class,
                "project_key = ? and repository_uri = ?", "JST", "owner/slug/default")).
                thenReturn(new ProjectMapping[]{projectMapping});
        when(activeObjects.find(IssueMapping.class,
                "issue_id = ?", "JST-1")).thenReturn(new IssueMapping[]{issueMapping});
        when(issueMapping.getNode()).thenReturn("1");
        when(issueMapping.getRepositoryUri()).thenReturn("owner/slug/default");
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).getChangesets("JST-1");
        verify(activeObjects, times(1)).find(ProjectMapping.class,
                "project_key = ? and repository_uri = ?", "JST", "owner/slug/default");
        verify(activeObjects, times(1)).find(IssueMapping.class,
                "issue_id = ?", "JST-1");
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
        when(activeObjects.find(ProjectMapping.class,
                "project_key = ? and repository_uri = ?",
                "JST", "owner/slug/default")).thenReturn(new ProjectMapping[]{projectMapping});
        when(activeObjects.find(IssueMapping.class,
                "issue_id = ?", "JST-1")).thenReturn(new IssueMapping[]{issueMapping});
        when(projectMapping.getUsername()).thenReturn("user");
        when(projectMapping.getPassword()).thenReturn("pass");
        when(issueMapping.getNode()).thenReturn("1");
        when(issueMapping.getRepositoryUri()).thenReturn("owner/slug/default");
        new DefaultBitbucketMapper(activeObjects, bitbucket, encryptor).getChangesets("JST-1");
        verify(activeObjects, times(1)).find(ProjectMapping.class,
                "project_key = ? and repository_uri = ?",
                "JST", "owner/slug/default");
        verify(activeObjects, times(1)).find(IssueMapping.class,
                "issue_id = ?", "JST-1");
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
        verify(activeObjects, times(1)).create(eq(IssueMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("repository_owner").equals("owner") &&
                                map.get("repository_slug").equals("slug") &&
                                map.get("project_key").equals("JST") &&
                                map.get("node").equals("1") &&
                                map.get("issue_id").equals("JST-1");
                    }
                }));
    }
}
