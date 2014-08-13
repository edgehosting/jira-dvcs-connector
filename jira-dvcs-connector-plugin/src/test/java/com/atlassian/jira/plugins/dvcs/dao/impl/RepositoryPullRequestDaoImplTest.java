package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestIssueKeyMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import net.java.ao.Query;
import net.java.ao.RawEntity;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyMapOf;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryPullRequestDaoImplTest
{
    private static final int REPO_ID = 1;

    @Mock
    private ActiveObjects activeObjects;

    @Mock
    private Repository repository;

    private RepositoryPullRequestDaoImpl dao;

    @BeforeMethod
    public void initializeMocksAndOrganizationDAO()
    {
        MockitoAnnotations.initMocks(this);
        dao = new RepositoryPullRequestDaoImpl(activeObjects);
    }

    @Test
    public void testUpdatePullRequestIssueKeysShouldUpdateFromTitleAndSourceBranch() throws Exception
    {
        mockNewPRWithCommits("TITLE-1 title", "SRCBR-1-source-branch", "TEST-2", "TEST-3");
        mockExistingPRIssueKeyMappings("TEST-2", "TEST-3");

        assertThat(dao.updatePullRequestIssueKeys(repository, REPO_ID), equalTo(4));
        // TODO use the matchers on master branch for more precise matching
        verify(activeObjects, times(2)).create(eq(RepositoryPullRequestIssueKeyMapping.class), anyMapOf(String.class, Object.class));
        verify(activeObjects, times(0)).delete(Matchers.<RawEntity<?>[]>anyVararg());
    }

    @Test
    public void testUpdatePullRequestIssueKeysShouldAddNewIssueKeyFromCommits() throws Exception
    {
        mockNewPRWithCommits("TITLE-1 title", "SRCBR-1-source-branch", "TEST-1", "TEST-2", "TEST-3");
        mockExistingPRIssueKeyMappings("TEST-2", "TEST-3");

        assertThat(dao.updatePullRequestIssueKeys(repository, REPO_ID), equalTo(5));
        verify(activeObjects, times(3)).create(eq(RepositoryPullRequestIssueKeyMapping.class), anyMapOf(String.class, Object.class));
        verify(activeObjects, times(0)).delete(Matchers.<RawEntity<?>[]>anyVararg());
    }

    @Test
    public void testUpdatePullRequestIssueKeysShouldDeleteOutdatedIssueKeys() throws Exception
    {
        mockNewPRWithCommits("TITLE-1 title", "SRCBR-1-source-branch", "TEST-1", "TEST-2", "TEST-3");
        mockExistingPRIssueKeyMappings("TEST-2", "TEST-3", "TEST-4", "TEST-5");

        assertThat(dao.updatePullRequestIssueKeys(repository, REPO_ID), equalTo(5));
        verify(activeObjects, times(3)).create(eq(RepositoryPullRequestIssueKeyMapping.class), anyMapOf(String.class, Object.class));
        verify(activeObjects, times(2)).delete(Matchers.<RawEntity<?>[]>anyVararg());
    }

    private RepositoryPullRequestMapping mockNewPRWithCommits(final String title, final String sourceBranch, String... issueKeys)
    {
        RepositoryPullRequestMapping mapping = mock(RepositoryPullRequestMapping.class);
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