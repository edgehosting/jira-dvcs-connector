package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
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
import org.hamcrest.Description;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.isSelect;
import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.withWhereParamsThat;
import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.withWhereThat;
import static com.google.common.collect.Iterables.toArray;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Listeners(MockitoTestNgListener.class)
public class RepositoryPullRequestDaoImplTest
{
    @Mock
    ActiveObjects activeObjects;

    @Mock
    ThreadEvents threadEvents;

    @Mock
    Repository repository;

    @InjectMocks
    RepositoryPullRequestDaoImpl repositoryPullRequestDao;

    @Test
    public void testGetIssueKeysWithExistingPullRequestIssueMappings()
    {
        RepositoryPullRequestIssueKeyMapping[] mappingsInDb = toArray(ImmutableList.of(
                newIssueMapping(1, "ISSUE-1"),
                newIssueMapping(1, "ISSUE-2"),
                newIssueMapping(1, "ISSUE-3")),
                RepositoryPullRequestIssueKeyMapping.class
        );
        when(activeObjects.find(any(Class.class), any(Query.class))).thenReturn(mappingsInDb);

        Set<String> result = repositoryPullRequestDao.getIssueKeys(1, 1);

        assertThat("Result should be never null", result, notNullValue());
        assertThat("Result should contain 3 items", result, hasSize(3));
        assertThat(result, containsInAnyOrder("ISSUE-1", "ISSUE-2", "ISSUE-3"));
        verify(activeObjects).find(eq(RepositoryPullRequestIssueKeyMapping.class), argThat(Matchers.<Query>allOf(
                isSelect(),
                withWhereThat(containsString(RepositoryPullRequestIssueKeyMapping.DOMAIN)),
                withWhereThat(containsString(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID)),
                withWhereParamsThat(Matchers.<Object>contains(1, 1))
        )));
    }

    @Test
    public void testGetIssueKeysWithNoExistingPullRequestIssueMappings()
    {
        when(activeObjects.find(any(Class.class), any(Query.class)))
                .thenReturn(new RepositoryPullRequestIssueKeyMapping[0]);

        Set<String> result = repositoryPullRequestDao.getIssueKeys(1, 1);

        assertThat("Result should be never null", result, notNullValue());
        assertThat("Result should be empty", result, hasSize(0));
        verify(activeObjects).find(eq(RepositoryPullRequestIssueKeyMapping.class), argThat(Matchers.<Query>allOf(
                isSelect(),
                withWhereThat(containsString(RepositoryPullRequestIssueKeyMapping.DOMAIN)),
                withWhereThat(containsString(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID)),
                withWhereParamsThat(Matchers.<Object>contains(1, 1))
        )));
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
                ((TransactionCallback)(invocation.getArguments()[0])).doInTransaction();
                return null;
            }
        });

        repositoryPullRequestDao.savePullRequest(originalPR);

        verify(activeObjects).create(eq(RepositoryPullRequestMapping.class), matchMap());
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
}
