package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.ao.EntityBeanGenerator;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.activity.RepositoryDomainMapping.DOMAIN;
import static com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping.ISSUE_KEY;
import static com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID;
import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.isSelect;
import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.withWhereOnColumns;
import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.withWhereParamsThat;
import static com.google.common.collect.Iterables.toArray;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners(MockitoTestNgListener.class)
public class RepositoryPullRequestDaoImplTest
{
    private static final int REPO_ID = 1;
    private static final int PR_ID = 1;

    @Mock
    ActiveObjects activeObjects;

    @Mock
    ThreadEvents threadEvents;

    @Mock
    Repository repository;

    @InjectMocks
    RepositoryPullRequestDaoImpl repositoryPullRequestDao;

    @BeforeMethod
    public void setUp() throws Exception
    {
        when(repository.getId()).thenReturn(REPO_ID);
    }

    @Test
    public void testGetIssueKeysWithExistingPullRequestIssueMappings()
    {
        RepositoryPullRequestIssueKeyMapping[] mappingsInDb = toArray(ImmutableList.of(
                newIssueMapping(PR_ID, "ISSUE-1"),
                newIssueMapping(PR_ID, "ISSUE-2"),
                newIssueMapping(PR_ID, "ISSUE-3")),
                RepositoryPullRequestIssueKeyMapping.class
        );
        when(activeObjects.find(any(Class.class), any(Query.class))).thenReturn(mappingsInDb);

        Set<String> result = repositoryPullRequestDao.getIssueKeys(REPO_ID, PR_ID);

        assertThat("Result should be never null", result, notNullValue());
        assertThat("Result should contain 3 items", result, hasSize(3));
        assertThat(result, containsInAnyOrder("ISSUE-1", "ISSUE-2", "ISSUE-3"));
        verify(activeObjects).find(eq(RepositoryPullRequestIssueKeyMapping.class), matchQueryForPRIssueKey());
    }

    @Test
    public void testGetIssueKeysWithNoExistingPullRequestIssueMappings()
    {
        when(activeObjects.find(any(Class.class), any(Query.class)))
                .thenReturn(new RepositoryPullRequestIssueKeyMapping[0]);

        Set<String> result = repositoryPullRequestDao.getIssueKeys(REPO_ID, PR_ID);

        assertThat("Result should be never null", result, notNullValue());
        assertThat("Result should be empty", result, hasSize(0));
        verify(activeObjects).find(eq(RepositoryPullRequestIssueKeyMapping.class), matchQueryForPRIssueKey());
    }

    @Test
    public void testSavePullRequestShouldConvertPRObjectToMapWithProperSize() throws Exception
    {
        EntityBeanGenerator beanGenerator = new EntityBeanGenerator();
        RepositoryPullRequestMapping originalPR = beanGenerator.createInstanceOf(RepositoryPullRequestMapping.class);
        when(activeObjects.executeInTransaction(any(TransactionCallback.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(final InvocationOnMock invocation) throws Throwable
            {
                ((TransactionCallback) (invocation.getArguments()[0])).doInTransaction();
                return null;
            }
        });

        repositoryPullRequestDao.savePullRequest(originalPR);

        verify(activeObjects).create(eq(RepositoryPullRequestMapping.class), matchMap());
    }

    @Test
    public void testUpdatePullRequestIssueKeysShouldUpdateFromTitleAndSourceBranch() throws Exception
    {
        mockNewPRWithCommits("TITLE-1 title", "SRCBR-1-source-branch", "TEST-2", "TEST-3");
        mockExistingPRIssueKeyMappings("TEST-2", "TEST-3");

        assertThat(repositoryPullRequestDao.updatePullRequestIssueKeys(repository, REPO_ID), equalTo(4));
        verify(activeObjects).create(eq(RepositoryPullRequestIssueKeyMapping.class), matchMapHasIssueKey("TITLE-1"));
        verify(activeObjects).create(eq(RepositoryPullRequestIssueKeyMapping.class), matchMapHasIssueKey("SRCBR-1"));
        verify(activeObjects, never()).delete(org.mockito.Matchers.<RawEntity<?>[]>anyVararg());
    }

    @Test
    public void testUpdatePullRequestIssueKeysShouldAddNewIssueKeyFromCommits() throws Exception
    {
        mockNewPRWithCommits("TITLE-1 title", "SRCBR-1-source-branch", "TEST-1", "TEST-2", "TEST-3");
        mockExistingPRIssueKeyMappings("TEST-2", "TEST-3");

        assertThat(repositoryPullRequestDao.updatePullRequestIssueKeys(repository, REPO_ID), equalTo(5));
        verify(activeObjects).create(eq(RepositoryPullRequestIssueKeyMapping.class), matchMapHasIssueKey("TITLE-1"));
        verify(activeObjects).create(eq(RepositoryPullRequestIssueKeyMapping.class), matchMapHasIssueKey("SRCBR-1"));
        verify(activeObjects).create(eq(RepositoryPullRequestIssueKeyMapping.class), matchMapHasIssueKey("TEST-1"));
        verify(activeObjects, never()).delete(org.mockito.Matchers.<RawEntity<?>[]>anyVararg());
    }

    @Test
    public void testUpdatePullRequestIssueKeysShouldDeleteOutdatedIssueKeys() throws Exception
    {
        mockNewPRWithCommits("TITLE-1 title", "SRCBR-1-source-branch", "TEST-1", "TEST-2", "TEST-3");
        mockExistingPRIssueKeyMappings("TEST-2", "TEST-3", "TEST-4", "TEST-5");

        assertThat(repositoryPullRequestDao.updatePullRequestIssueKeys(repository, REPO_ID), equalTo(5));
        verify(activeObjects).create(eq(RepositoryPullRequestIssueKeyMapping.class), matchMapHasIssueKey("TITLE-1"));
        verify(activeObjects).create(eq(RepositoryPullRequestIssueKeyMapping.class), matchMapHasIssueKey("SRCBR-1"));
        verify(activeObjects).create(eq(RepositoryPullRequestIssueKeyMapping.class), matchMapHasIssueKey("TEST-1"));
        verify(activeObjects, times(2)).delete(org.mockito.Matchers.<RawEntity<?>[]>anyVararg());
        // the find used by delete
        verify(activeObjects).find(eq(RepositoryPullRequestIssueKeyMapping.class), matchQueryForPRIssueKeyWithIssueKey("TEST-4"));
        verify(activeObjects).find(eq(RepositoryPullRequestIssueKeyMapping.class), matchQueryForPRIssueKeyWithIssueKey("TEST-5"));
    }

    private Query matchQueryForPRIssueKeyWithIssueKey(final String issueKey)
    {
        Matcher<Query> matcher = allOf(
                isSelect(),
                withWhereOnColumns(DOMAIN, PULL_REQUEST_ID, ISSUE_KEY),
                withWhereParamsThat(Matchers.<Object>contains(REPO_ID, PR_ID, issueKey))
        );
        return argThat(matcher);
    }

    private Query matchQueryForPRIssueKey()
    {
        Matcher<Query> matcher = allOf(
                isSelect(),
                withWhereOnColumns(DOMAIN, PULL_REQUEST_ID),
                withWhereParamsThat(Matchers.<Object>contains(REPO_ID, PR_ID))
        );
        return argThat(matcher);
    }

    private Map<String, Object> matchMapHasIssueKey(String issueKey)
    {
        //noinspection unchecked
        return (Map)argThat(hasEntry(ISSUE_KEY, (Object) issueKey));
    }

    private Map<String, Object> matchMap()
    {
        final List<String> expectedFields = getStaticStringFieldNamesExcluding(RepositoryPullRequestMapping.class,
                RepositoryPullRequestMapping.PARTICIPANTS);
        return argThat(new TypeSafeMatcher<Map<String, Object>>() {

            @Override
            protected boolean matchesSafely(final Map<String, Object> item)
            {
                return expectedFields.size() == item.size();
            }

            @Override
            public void describeTo(final Description description)
            {
                description.appendText("a map with " + expectedFields.size() + " items: keys=" + expectedFields);
            }
        });
    }

    private List<String> getStaticStringFieldNamesExcluding(final Class<RepositoryPullRequestMapping> clazz, final String... names)
    {
        return ImmutableList.copyOf(Iterables.transform(Iterables.filter(getDeclaredFields(clazz), new Predicate<Field>()
        {
            @Override
            public boolean apply(@Nullable final Field input)
            {
                if (Modifier.isStatic(input.getModifiers()) && input.getDeclaringClass().equals(String.class))
                {
                    return false;
                }
                for (String name : names)
                {
                    if (input.getName().equals(name))
                    {
                        return false;
                    }
                }
                return true;
            }
        }), new Function<Field, String>()
        {
            @Override
            public String apply(@Nullable final Field input)
            {
                return input.getName();
            }
        }));
    }

    /**
     * Borrowed from {@code com.atlassian.plugins.rest.common.util.ReflectionUtils#getDeclaredFields()}
     * @param clazz
     * @return
     */
    private List<Field> getDeclaredFields(Class clazz)
    {
        if (clazz == null)
        {
            return Lists.newArrayList();
        }
        else
        {
            final List<Field> superFields = getDeclaredFields(clazz.getSuperclass());
            for (Class superClazz : clazz.getInterfaces())
            {
                superFields.addAll(getDeclaredFields(superClazz));
            }
            superFields.addAll(Arrays.asList(clazz.getDeclaredFields()));
            return superFields;
        }
    }

    private RepositoryPullRequestIssueKeyMapping newIssueMapping(int prId, String issueKey)
    {
        RepositoryPullRequestIssueKeyMapping mapping = mock(RepositoryPullRequestIssueKeyMapping.class);
        when(mapping.getPullRequestId()).thenReturn(prId);
        when(mapping.getIssueKey()).thenReturn(issueKey);
        return mapping;
    }

    private RepositoryPullRequestMapping mockNewPRWithCommits(final String title, final String sourceBranch, String... issueKeys)
    {
        RepositoryPullRequestMapping mapping = mock(RepositoryPullRequestMapping.class);
        when(mapping.getID()).thenReturn(PR_ID);
        when(mapping.getName()).thenReturn(title);
        when(mapping.getSourceBranch()).thenReturn(sourceBranch);

        RepositoryCommitMapping[] commitMappings = new RepositoryCommitMapping[issueKeys.length];
        for (int i = 0; i < issueKeys.length; i++)
        {
            commitMappings[i] = mock(RepositoryCommitMapping.class);
            when(commitMappings[i].getMessage()).thenReturn(issueKeys[i] + " commit msg " + i);
        }
        when(mapping.getCommits()).thenReturn(commitMappings);

        when(activeObjects.get(eq(RepositoryPullRequestMapping.class), eq(REPO_ID))).thenReturn(mapping);

        return mapping;
    }

    private RepositoryPullRequestIssueKeyMapping[] mockExistingPRIssueKeyMappings(String... issueKeys)
    {
        RepositoryPullRequestIssueKeyMapping[] mappings = new RepositoryPullRequestIssueKeyMapping[issueKeys.length];
        for (int i = 0; i < issueKeys.length; i++)
        {
            mappings[i] = mock(RepositoryPullRequestIssueKeyMapping.class);
            when(mappings[i].getIssueKey()).thenReturn(issueKeys[i]);
        }

        when(activeObjects.find(eq(RepositoryPullRequestIssueKeyMapping.class), any(Query.class))).thenReturn(mappings);

        return mappings;
    }
}
