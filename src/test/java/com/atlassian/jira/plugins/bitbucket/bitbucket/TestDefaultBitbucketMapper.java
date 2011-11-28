package com.atlassian.jira.plugins.bitbucket.bitbucket;

import static junit.framework.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultRepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl.BitbucketRepositoryUri;
import com.atlassian.sal.api.transaction.TransactionCallback;

/**
 * Unit tests for {@link DefaultRepositoryPersister}
 */
@SuppressWarnings("unchecked")
public class TestDefaultBitbucketMapper
{
    private static final String URL = "https://bitbucket.org/owner/slug";
    private static final RepositoryUri REPOSITORY_URI = new BitbucketRepositoryUri("https","bitbucket.org","owner","slug");
	private static final int SOME_ID = 123;
	@Mock
    ActiveObjects activeObjects;
    @Mock
    Communicator bitbucket;
    @Mock
    ProjectMapping projectMapping;
    @Mock
    IssueMapping issueMapping;
    @Mock
    Changeset changeset;
    @Mock
    Encryptor encryptor;

    @Before 
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
        when(projectMapping.getID()).thenReturn(SOME_ID);
        when(projectMapping.getRepositoryUrl()).thenReturn(URL);
        when(activeObjects.find(ProjectMapping.class, "PROJECT_KEY = ?", "JST")).thenReturn(new ProjectMapping[]{projectMapping});
        when(activeObjects.find(ProjectMapping.class, "REPOSITORY_URL = ? and PROJECT_KEY = ?","https://bitbucket.org/owner/slug", "JST")).thenReturn(new ProjectMapping[]{});
        when(activeObjects.create(eq(ProjectMapping.class), anyMap())).thenReturn(projectMapping);
        when(changeset.getBranch()).thenReturn("default");
        when(changeset.getNode()).thenReturn("1");
        when(changeset.getRepositoryId()).thenReturn(SOME_ID);
        //noinspection unchecked
        when(activeObjects.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(
                new Answer<Object>()
                {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
                    }
                }
        );
        when(encryptor.encrypt(anyString(), anyString(), anyString())).thenAnswer(
                new Answer<String>()
                {
                    @Override
                    public String answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return StringUtils.reverse(String.valueOf(invocationOnMock.getArguments()[0]));
                    }
                }
        );
        when(encryptor.decrypt(anyString(), anyString(), anyString())).thenAnswer(
                new Answer<String>()
                {
                    @Override
                    public String answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return StringUtils.reverse(String.valueOf(invocationOnMock.getArguments()[0]));
                    }
                }
        );
    }

    @Test
    public void testAddAnonymousRepositoryCreatesValidMap()
    {
        new DefaultRepositoryPersister(activeObjects).
                addRepository("bitbucket", "JST", REPOSITORY_URI.getRepositoryUrl(), null, null, null, null, null);
        verify(activeObjects, times(1)).create(eq(ProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    @Override
					public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("REPOSITORY_URL").equals(URL) &&
                                map.get("PROJECT_KEY").equals("JST") &&
                                !map.containsKey("USERNAME") && !map.containsKey("PASSWORD");
                    }
                }));
    }

    @Test
    public void testAddAuthentictedRepositoryCreatesValidMap()
    {
        new DefaultRepositoryPersister(activeObjects).
                addRepository("bitbucket", "JST", REPOSITORY_URI.getRepositoryUrl(), "user", "pass", null, null, null);
        verify(activeObjects, times(1)).create(eq(ProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    @Override
					public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("REPOSITORY_URL").equals(URL) &&
                                map.get("PROJECT_KEY").equals("JST") &&
                                map.get("USERNAME").equals("user") &&
                                map.containsKey("PASSWORD");
                    }
                }));
    }

    @Test
    public void testPasswordNotStoredInPlainText()
    {
    	new BitbucketRepositoryManager(new DefaultRepositoryPersister(activeObjects), bitbucket, encryptor, null)
    		.addRepository("bitbucket", "JST", REPOSITORY_URI.getRepositoryUrl(), "user", "pass", null, null, "");
        verify(activeObjects, times(1)).create(eq(ProjectMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    @Override
					public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return !map.get("PASSWORD").equals("pass");
                    }
                }));
        verify(encryptor, times(1)).encrypt("pass", "JST", "https://bitbucket.org/owner/slug");
    }

    @Test
    public void testRemoveRepositoryAlsoRemovesIssues()
    {
		when(activeObjects.get(ProjectMapping.class, SOME_ID)).thenReturn(projectMapping);
		when(activeObjects.find(IssueMapping.class, "REPOSITORY_ID = ?", SOME_ID)).thenReturn(new IssueMapping[] { issueMapping });

		new DefaultRepositoryPersister(activeObjects).removeRepository(SOME_ID);
        verify(activeObjects, times(1)).get(ProjectMapping.class, SOME_ID);
		verify(activeObjects, times(1)).find(IssueMapping.class, "REPOSITORY_ID = ?", SOME_ID);
        verify(activeObjects, times(1)).delete(projectMapping);
        verify(activeObjects, times(1)).delete(issueMapping);
    }

    @Test
    public void testGetChangesets()
    {
        when(activeObjects.find(IssueMapping.class, "ISSUE_ID = ?", "JST-1"))
            .thenReturn(new IssueMapping[] { issueMapping });
        when(activeObjects.find(ProjectMapping.class, "REPOSITORY_TYPE = ?", "bitbucket"))
            .thenReturn(new ProjectMapping[] { projectMapping });
        when(issueMapping.getNode()).thenReturn("1");
        when(issueMapping.getRepositoryId()).thenReturn(SOME_ID);
        List<IssueMapping> issueMappings = new DefaultRepositoryPersister(activeObjects).getIssueMappings("JST-1", "bitbucket");
        assertTrue(issueMappings.size()==1);
        assertEquals(issueMapping, issueMappings.get(0));
//        verify(activeObjects, times(1)).find(ProjectMapping.class,
//                "PROJECT_KEY = ? and REPOSITORY_URI = ?", "JST", "owner/slug");
//        verify(activeObjects, times(1)).find(IssueMapping.class,
//                "ISSUE_ID = ?", "JST-1");
//        verify(bitbucket, times(1)).getChangeset(argThat(new ArgumentMatcher<Authentication>()
//        {
//            @Override
//            public boolean matches(Object o)
//            {
//                return o == Authentication.ANONYMOUS;
//            }
//        }), eq("owner"), eq("slug"), eq("1"));
    }

    @Test
    public void testGetChangesetsOnAuthenticatedRepository()
    {
        when(activeObjects.find(IssueMapping.class, "ISSUE_ID = ?", "JST-1"))
            .thenReturn(new IssueMapping[] { issueMapping });
        when(activeObjects.find(ProjectMapping.class, "REPOSITORY_TYPE = ?", "bitbucket"))
            .thenReturn(new ProjectMapping[] { projectMapping });
        when(projectMapping.getUsername()).thenReturn("user");
        when(projectMapping.getPassword()).thenReturn("ssap");
        when(issueMapping.getNode()).thenReturn("1");
        when(issueMapping.getRepositoryId()).thenReturn(SOME_ID);
        List<IssueMapping> issueMappings = new DefaultRepositoryPersister(activeObjects).getIssueMappings("JST-1", "bitbucket");
        assertTrue(issueMappings.size()==1);
        assertEquals(issueMapping, issueMappings.get(0));
//        verify(activeObjects, times(1)).find(ProjectMapping.class,
//                "PROJECT_KEY = ? and REPOSITORY_URI = ?",
//                "JST", "owner/slug");
//        verify(activeObjects, times(1)).find(IssueMapping.class,
//                "ISSUE_ID = ?", "JST-1");
//        verify(bitbucket, times(1)).getChangeset(argThat(new ArgumentMatcher<Authentication>()
//        {
//            @Override
//            public boolean matches(Object o)
//            {
//                BasicAuthentication auth = (BasicAuthentication) o;
//                return auth.getUsername().equals("user") && auth.getPassword().equals("pass");
//            }
//        }), eq("owner"), eq("slug"), eq("1"));
    }

    @Test
    public void testAddChangesetToSameBranch()
    {
        when(activeObjects.get(ProjectMapping.class,SOME_ID)).thenReturn(projectMapping);

        new DefaultRepositoryPersister(activeObjects).addChangeset("JST-1", changeset);
        verify(activeObjects, times(1)).create(eq(IssueMapping.class),
                argThat(new ArgumentMatcher<Map<String, Object>>()
                {
                    @Override
					public boolean matches(Object o)
                    {
                        //noinspection unchecked
                        Map<String, Object> map = (Map<String, Object>) o;
                        return map.get("REPOSITORY_ID").equals(SOME_ID) &&
                                map.get("NODE").equals("1") &&
                                map.get("ISSUE_ID").equals("JST-1");
                    }
                }));
    }
}
