package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.java.ao.Query;
import org.hamcrest.Matchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Set;

import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.isSelect;
import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.withWhereParamsThat;
import static com.atlassian.jira.plugins.dvcs.matchers.QueryMatchers.withWhereThat;
import static com.google.common.collect.Iterables.toArray;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
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

    @Mock
    RepositoryPullRequestMapping repositoryPullRequestMapping;

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

        assertNotNull("Result should be never null", result);
        assertEquals(ImmutableSet.of("ISSUE-1", "ISSUE-2", "ISSUE-3"), result);
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

        assertNotNull("Result should be never null", result);
        assertTrue("Result should be empty", result.isEmpty());
        verify(activeObjects).find(eq(RepositoryPullRequestIssueKeyMapping.class), argThat(Matchers.<Query>allOf(
                isSelect(),
                withWhereThat(containsString(RepositoryPullRequestIssueKeyMapping.DOMAIN)),
                withWhereThat(containsString(RepositoryPullRequestIssueKeyMapping.PULL_REQUEST_ID)),
                withWhereParamsThat(Matchers.<Object>contains(1, 1))
        )));
    }

    private RepositoryPullRequestIssueKeyMapping newIssueMapping(int prId, String issueKey)
    {
        RepositoryPullRequestIssueKeyMapping mapping = mock(RepositoryPullRequestIssueKeyMapping.class);
        when(mapping.getPullRequestId()).thenReturn(prId);
        when(mapping.getIssueKey()).thenReturn(issueKey);
        return mapping;
    }
}
