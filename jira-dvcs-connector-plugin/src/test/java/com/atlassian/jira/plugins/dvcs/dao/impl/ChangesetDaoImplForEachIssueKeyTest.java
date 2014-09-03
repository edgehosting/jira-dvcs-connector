package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.QueryHelper;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.IssueToChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.IssueToMappingFunction;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import net.java.ao.Query;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class ChangesetDaoImplForEachIssueKeyTest
{
    private static final String DVCS_TYPE = "bitbucket";
    private static final int REPOSITORY_ID = 1;
    private static final int PAGE_SIZE = 100;
    private static final java.lang.String ISSUE_KEY = "TEST-1";

    @Mock
    private ActiveObjects activeObjects;

    @Mock
    private QueryHelper queryHelper;

    @Mock
    private Changeset changeset;

    @Mock
    private OrganizationMapping organizationMapping;

    @Mock
    private RepositoryMapping repositoryMapping;

    @Mock
    private IssueToChangesetMapping issueToChangesetMapping;

    @Mock
    private IssueToMappingFunction closure;

    @Captor
    private ArgumentCaptor<Set<String>> captor;

    @InjectMocks
    private ChangesetDaoImpl changesetDao;

    @BeforeMethod
    public void setUp() throws Exception
    {
        when(issueToChangesetMapping.getIssueKey()).thenReturn(ISSUE_KEY);
        when(activeObjects.find(eq(OrganizationMapping.class), any(Query.class))).thenReturn(new OrganizationMapping[] { organizationMapping });
        when(activeObjects.find(eq(RepositoryMapping.class), any(Query.class))).thenReturn(new RepositoryMapping[] { repositoryMapping });
    }

    @Test
    public void testIssueKeyPageSuccess() throws Exception
    {
        setupForSuccessFlow();

        boolean result = changesetDao.processIssueKeyPage(DVCS_TYPE, REPOSITORY_ID, PAGE_SIZE, closure);
        assertThat(result, is(true));
        assertThat(2, is(captor.getAllValues().size()));
        assertThat(captor.getAllValues().get(0), contains(ISSUE_KEY));
    }

    @Test
    public void testOverallProcessSuccess()
    {
        setupForSuccessFlow();

        boolean result = changesetDao.forEachIssueToCommitMapping(closure);
        assertThat(result, is(true));
        assertThat(2, is(captor.getAllValues().size()));
        assertThat(captor.getAllValues().get(0), contains(ISSUE_KEY));
    }

    private void setupForSuccessFlow()
    {
        final IssueToChangesetMapping[] returnedMappings = new IssueToChangesetMapping[] { issueToChangesetMapping };
        when(activeObjects.find(eq(IssueToChangesetMapping.class), any(Query.class)))
                .thenReturn(returnedMappings).thenReturn(new IssueToChangesetMapping[0]);
        when(closure.execute(any(String.class), anyInt(), captor.capture())).thenReturn(true);
    }

    @Test
    public void testIssueKeyPageStop() throws Exception
    {
        setupForStopFlow();

        boolean result = changesetDao.processIssueKeyPage(DVCS_TYPE, REPOSITORY_ID, PAGE_SIZE, closure);
        assertThat(result, is(false));
    }

    @Test
    public void testOverallProcessClojureStop()
    {
        setupForStopFlow();

        boolean result = changesetDao.forEachIssueToCommitMapping(closure);
        assertThat(result, is(false));
    }

    private void setupForStopFlow()
    {
        final IssueToChangesetMapping[] returnedMappings = new IssueToChangesetMapping[] { issueToChangesetMapping };
        // Can run forever so make it throw an exception on second call that should never happen
        when(activeObjects.find(eq(IssueToChangesetMapping.class), any(Query.class))).thenReturn(returnedMappings).thenThrow(new RuntimeException());
        when(closure.execute(any(String.class), anyInt(), any(Set.class))).thenReturn(false);
    }
}
