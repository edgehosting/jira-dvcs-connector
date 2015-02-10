package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.GitHubEventMapping;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.MockitoTestNgListener;
import net.java.ao.Query;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

@Listeners (MockitoTestNgListener.class)
public class GitHubEventDaoImplTest
{
    private static final String GIT_HUB_ID = "123";
    private static final int REPOSITORY_ID = 234;
    @Mock
    private ActiveObjects activeObjects;

    @Mock
    private GitHubEventMapping gitHubEventMapping;

    @Mock
    private GitHubEventMapping gitHubEventMapping2;

    @Mock
    private Repository repository;

    @InjectMocks
    private GitHubEventDAOImpl gitHubEventDAO;

    @BeforeMethod
    public void setup()
    {
        when(repository.getId()).thenReturn(REPOSITORY_ID);
    }

    @Test
    public void testRetrieveSingleGithubEvent()
    {
        when(activeObjects.find(any(Class.class), any(Query.class))).thenReturn(new GitHubEventMapping[]{gitHubEventMapping});

        GitHubEventMapping retrievedMapping = gitHubEventDAO.getByGitHubId(repository, GIT_HUB_ID);
        assertThat(retrievedMapping, equalTo(gitHubEventMapping));
    }

    @Test
    public void testRetrieveMultipleGithubEventFoundReturnsFirst()
    {
        final GitHubEventMapping[] gitHubEventMappings = { gitHubEventMapping2, gitHubEventMapping };
        when(activeObjects.find(any(Class.class), any(Query.class))).thenReturn(gitHubEventMappings);

        GitHubEventMapping retrievedMapping = gitHubEventDAO.getByGitHubId(repository, GIT_HUB_ID);
        assertThat(retrievedMapping, equalTo(gitHubEventMapping2));
    }

    @Test
    public void testRetrieveMultipleGithubEventFoundReturnsSavepoint()
    {
        when(gitHubEventMapping.isSavePoint()).thenReturn(true);
        final GitHubEventMapping[] gitHubEventMappings = { gitHubEventMapping2, gitHubEventMapping };
        when(activeObjects.find(any(Class.class), any(Query.class))).thenReturn(gitHubEventMappings);

        GitHubEventMapping retrievedMapping = gitHubEventDAO.getByGitHubId(repository, GIT_HUB_ID);
        assertThat(retrievedMapping, equalTo(gitHubEventMapping));
    }
}
