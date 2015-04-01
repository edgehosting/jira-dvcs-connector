package com.atlassian.jira.plugins.dvcs.rest;

import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.issue.MutableIssue;
import com.atlassian.jira.issue.changehistory.ChangeHistoryManager;
import com.atlassian.jira.mock.issue.MockIssue;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestChangeset;
import com.atlassian.jira.plugins.dvcs.model.dev.RestChangesetRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.rest.security.AuthorizationException;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.webwork.IssueAndProjectKeyManager;
import com.atlassian.jira.plugins.dvcs.webwork.IssueAndProjectKeyManagerImpl;
import com.atlassian.jira.project.MockProject;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.security.JiraAuthenticationContext;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.plugin.ProjectPermissionKey;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.MockApplicationUser;
import com.atlassian.plugins.rest.common.Status;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.Matchers;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import javax.ws.rs.core.Response;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Test for commits Restfull endpoint
 *
 */
public class RestCommitsTest
{
    private DevToolsResource devToolsResource;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private ChangesetService changesetService;

    @Mock
    private PullRequestService pullRequestService;

    @Mock
    private BranchService branchService;

    @Mock
    private IssueManager issueManager;

    @Mock
    private ChangeHistoryManager changeHistoryManager;

    @Mock
    private ProjectManager projectManager;

    @Mock
    private PermissionManager permissionManager;

    @Mock
    private JiraAuthenticationContext jiraAuthenticationContext;

    private IssueAndProjectKeyManager issueAndProjectKeyManager;

    private int issueIdSequence;
    private int projectIdSequence;

    @BeforeMethod (alwaysRun=true)
    public void setup()
    {
        MockitoAnnotations.initMocks(this);

        issueAndProjectKeyManager = new IssueAndProjectKeyManagerImpl(issueManager, changeHistoryManager, projectManager, permissionManager, jiraAuthenticationContext);

        when(jiraAuthenticationContext.getLoggedInUser()).thenReturn(new MockApplicationUser("USER", "FULLNAME", "EMAIL"));
        when(permissionManager.hasPermission(any(ProjectPermissionKey.class), argThat(new ProjectArgumentMatcher("TST")), argThat(new UserArgumentMatcher("USER")))).thenReturn(true);
        when(permissionManager.hasPermission(any(ProjectPermissionKey.class), argThat(new ProjectArgumentMatcher("FORBIDDEN")), argThat(new UserArgumentMatcher("USER")))).thenReturn(false);

        RepositoryBuilder repositoryBuilder = new RepositoryBuilder();

        repositoryBuilder.addRepository(1);
        repositoryBuilder.addRepository(2, 1);
        repositoryBuilder.addRepository(3, 2);

        repositoryBuilder.addChangeset(1, "TST-1", false, true, 1, 2, 3);
        repositoryBuilder.addChangeset(2, "TST-1", true, true, 1, 2);
        repositoryBuilder.addChangeset(3, "TST-1", false, true, 1);
        repositoryBuilder.addChangeset(4, "TST-2", false, false, 1);
        // has permission on issue, but not on project
        repositoryBuilder.addChangeset(5, "FORBIDDEN-1", false, true, 1);
        // Should not be displayed with any repository as we will not ask for such issue keys
        repositoryBuilder.addChangeset(6, "TST-3", false, true, 2);
        repositoryBuilder.addChangeset(7, "TST-4", false, true, 3);
        repositoryBuilder.mock();

        when(repositoryService.getUser(any(Repository.class), anyString(), anyString())).thenReturn(new DvcsUser("USERNAME", "FULL_NAME", "RAW_AUTHOR", "AVATAR", "URL"));

        devToolsResource = new DevToolsResource(repositoryService, changesetService, pullRequestService, branchService, issueAndProjectKeyManager);
    }

    class RepositoryBuilder
    {
        private ListMultimap<String, Changeset> changesetToIssues = ArrayListMultimap.create();
        private HashMap<String, Boolean> issuePermission = new HashMap<String, Boolean>();
        private HashMap<Integer, Repository> repositories = new HashMap<Integer, Repository>();

        public void addChangeset(int id, String issueKey, boolean merge, boolean hasPermission, int repositoryId, Integer... otherRepositoryIds)
        {
            // setting data
            List<String> parents = new ArrayList<String>();
            parents.add("PARENT_OF_"+id);
            if (merge)
            {
                parents.add("ANOTHER_PARENT_OF_"+id);
            }
            Changeset changeset = new Changeset(id,"0123456_NODE_" + id, "RAW_AUTHOR", "AUTHOR", new Date(), "RAW_NODE_"+id, "BRANCH", issueKey + " commit " + id, parents, Collections.singletonList(new ChangesetFile(ChangesetFileAction.ADDED, "FILE"+id)), 1, "AUTHOR_EMAIL");
            changeset.setRepositoryIds(Lists.asList(repositoryId, otherRepositoryIds));

            changesetToIssues.put(issueKey, changeset);
            issuePermission.put(issueKey, hasPermission);
        }

        public void addRepository(int id)
        {
            addRepository(id, null);
        }

        public void addRepository(int id, Integer forkOf)
        {
            final Repository repository = new Repository(id, 0, "git", "SLUG_" + id, "REPOSITORY_NAME_" + id, new Date(), true, false, null);
            if (forkOf != null)
            {
                repository.setFork(true);
                repository.setForkOf(repositories.get(forkOf));
            }
            repositories.put(id, repository);
        }

        public void mock()
        {
            for (String issueKey : changesetToIssues.keySet())
            {
                List<Changeset> changesets = changesetToIssues.get(issueKey);
                for (Changeset changeset : changesets)
                {
                    // setting permissions
                    String projectKey = issueKey.substring(0,issueKey.lastIndexOf('-'));
                    when(issueManager.getIssueObject(eq(issueKey))).thenReturn(mockIssue(++issueIdSequence,projectKey, issueKey));
                    when(issueManager.getAllIssueKeys(eq((long)issueIdSequence))).thenReturn(Collections.singleton(issueKey));
                    when(permissionManager.hasPermission(any(ProjectPermissionKey.class), argThat(new IssueArgumentMatcher(issueKey)), any(ApplicationUser.class))).thenReturn(issuePermission.get(issueKey));
                }

                when(changesetService.getByIssueKey((Iterable<String>) argThat(Matchers.<String>containsInAnyOrder(issueKey)), anyBoolean())).thenReturn(changesets);
            }

            for (Integer repositoryId : repositories.keySet())
            {
                when(repositoryService.get(eq(repositoryId))).thenReturn(repositories.get(repositoryId));
            }
        }
    }

    private MutableIssue mockIssue(int id, String projectKey, String issueKey)
    {
        MockIssue issue = new MockIssue(id, issueKey);
        issue.setProjectObject(new MockProject(++projectIdSequence, projectKey));
        return issue;
    }

    @AfterMethod
    public void tearDown()
    {
        ComponentAccessor.initialiseWorker(null); // reset
    }

    @Test
    public void testCommits()
    {
        Response response = devToolsResource.getCommits("TST-1");

        assertEquals(response.getStatus(), 200, "Status should be 200");
        RestDevResponse restChangesets = (RestDevResponse) response.getEntity();

        List<RestChangesetRepository> restRepositories = (List<RestChangesetRepository>) restChangesets.getRepositories();
        // checking repositories count
        assertThat(restRepositories, hasSize(3));

        // checking repositories
        checkRepository(restRepositories.get(0), 1, 3, null);
        checkRepository(restRepositories.get(1), 2, 2, "SLUG_1");
        checkRepository(restRepositories.get(2), 3, 1, "SLUG_2");

        // checking merge commits
        checkChangeset(restRepositories.get(0).getCommits().get(0), false, 1, "TST-1");
        checkChangeset(restRepositories.get(0).getCommits().get(1), true, 2, "TST-1");
        checkChangeset(restRepositories.get(0).getCommits().get(2), false, 3, "TST-1");

        checkChangeset(restRepositories.get(1).getCommits().get(0), false, 1, "TST-1");
        checkChangeset(restRepositories.get(1).getCommits().get(1), true, 2, "TST-1");

        checkChangeset(restRepositories.get(2).getCommits().get(0), false, 1, "TST-1");
    }

    private void checkRepository(RestChangesetRepository restRepository, int id, int commitCounts, String forkOf)
    {
        assertThat(restRepository.getCommits(), hasSize(commitCounts));
        assertEquals(restRepository.getSlug(), "SLUG_"+id);
        assertEquals(restRepository.getName(), "REPOSITORY_NAME_"+id);
        if (forkOf != null)
        {
            assertTrue(restRepository.isFork());
            assertEquals(restRepository.getForkOf().getSlug(), forkOf);
        } else
        {
            assertFalse(restRepository.isFork());
        }
    }

    private void checkChangeset(RestChangeset restChangeset, boolean isMerge, int id, String issueKey)
    {
        assertThat("Merge status is wrong", restChangeset.isMerge(), is(isMerge));
        assertThat(restChangeset.getId(), is("RAW_NODE_" + id));
        assertThat(restChangeset.getFileCount(), is(1));
        assertThat(restChangeset.getMessage(), is(issueKey + " commit " + id));
        assertThat(restChangeset.getDisplayId(), is("0123456"));
    }


    @Test
    public void testIssueNotFound()
    {
        Response response = devToolsResource.getCommits("TST-123");

        assertEquals(response.getStatus(), 404, "Status should be 404");
        assertEquals(response.getEntity().getClass(), Status.class);
    }

    @Test(expectedExceptions = AuthorizationException.class)
    public void testIssueNoPermission()
    {
        devToolsResource.getCommits("TST-2");
    }

    @Test(expectedExceptions = AuthorizationException.class)
    public void testProjectNoPermission()
    {
        devToolsResource.getCommits("FORBIDDEN-1");
    }
}

class ProjectArgumentMatcher extends ArgumentMatcher<Project>
{
    private String key;

    public ProjectArgumentMatcher(final String key)
    {
        this.key = key;
    }

    @Override
    public boolean matches(final Object argument)
    {
        if (argument instanceof Project)
        {
            Project project = (Project) argument;
            return StringUtils.equals(key, project.getKey());
        }
        return false;
    }
}

class UserArgumentMatcher extends ArgumentMatcher<ApplicationUser>
{
    private String name;

    public UserArgumentMatcher(final String name)
    {
        this.name = name;
    }

    @Override
    public boolean matches(final Object argument)
    {
        if (argument instanceof ApplicationUser)
        {
            ApplicationUser user = (ApplicationUser) argument;
            return StringUtils.equals(name, user.getName());
        }
        return false;
    }
}

class IssueArgumentMatcher extends ArgumentMatcher<Issue>
{
    private String key;

    public IssueArgumentMatcher(final String key)
    {
        this.key = key;
    }

    @Override
    public boolean matches(final Object argument)
    {
        if (argument instanceof Issue)
        {
            Issue issue = (Issue) argument;
            return StringUtils.equals(key, issue.getKey());
        }
        return false;
    }
}
