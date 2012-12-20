package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import static org.fest.assertions.api.Assertions.assertThat;
import it.com.atlassian.jira.plugins.dvcs.BitBucketBaseOrgTest;

import java.io.IOException;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.BaseConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraPageUtils;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.PostCommitHookCallSimulatingRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Martin Skurla
 */
public abstract class AbstractMissingCommitsTest<T extends BaseConfigureOrganizationsPage> extends BitBucketBaseOrgTest<T>
{
    static final String DVCS_REPO_OWNER = "dvcsconnectortest";
    static final String DVCS_REPO_PASSWORD = PasswordUtil.getPassword("dvcsconnectortest");
    static final String MISSING_COMMITS_REPOSITORY_NAME = "missingcommitsfixproof";

    private static final String JIRA_PROJECT_NAME_AND_KEY = "MC"; // Missing Commits


    @BeforeMethod
    public void prepareRemoteDvcsRepositoryAndJiraProjectWithIssue()
    {
        removeRemoteDvcsRepository();
        removeJiraProject();

        createRemoteDvcsRepository();
        createJiraProjectWithIssue();
    }

    abstract void removeRemoteDvcsRepository();
    abstract void createRemoteDvcsRepository();


    abstract void loginToDvcsAndSetJiraOAuthCredentials();
    abstract void pushToRemoteDvcsRepository(String pathToRepoZip) throws Exception;

    abstract String getFirstDvcsZipRepoPathToPush();
    abstract String getSecondDvcsZipRepoPathToPush();

    abstract void removeOAuth();
    
    @Test
    public void commitsIssueTab_ShouldNotMissAnyRelatedCommits() throws Exception
    {
        pushToRemoteDvcsRepository(getFirstDvcsZipRepoPathToPush());

        loginToDvcsAndSetJiraOAuthCredentials();
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + configureOrganizations.getUrl());
        configureOrganizations.addOrganizationSuccessfully(DVCS_REPO_OWNER, true);

        assertThat(getCommitsForIssue("MC-1", 3)).hasSize(3);

        pushToRemoteDvcsRepository(getSecondDvcsZipRepoPathToPush());

        simulatePostCommitHookCall();
        JiraPageUtils.checkSyncProcessSuccess(jira); // to catch up with soft sync
        
        assertThat(getCommitsForIssue("MC-1", 5)).hasSize(5);
        
        removeOAuth();
    }

    public String getGitCommand()
    {
        Process process;
        try
        {
         // executing "git" without any arguent on Win OS would wait forever (even if git is correctly placed on PATH)
            // => we need to execute "git" with some argument e.g. "--version"
            process = new ProcessBuilder("git", "--version").start();
            process.waitFor();
            return "git";
        } catch (Exception e)
        {
            return "/usr/local/git/bin/git";
        }
    }
    
    public String getHgCommand()
    {
        Process process;
        try
        {
            process = new ProcessBuilder("hg", "--version").start();
            process.waitFor();
            return "hg";
        } catch (Exception e)
        {
            return "/usr/local/bin/hg";
        }
    }
    
    private void removeJiraProject()
    {
        if (JiraPageUtils.projectExists(jira, JIRA_PROJECT_NAME_AND_KEY))
        {
            JiraPageUtils.deleteProject(jira, JIRA_PROJECT_NAME_AND_KEY);
        }
    }

    private void createJiraProjectWithIssue()
    {
        JiraPageUtils.createProject(jira, JIRA_PROJECT_NAME_AND_KEY, JIRA_PROJECT_NAME_AND_KEY);
        JiraPageUtils.createIssue(jira, JIRA_PROJECT_NAME_AND_KEY);
    }

    private void simulatePostCommitHookCall() throws IOException
    {
        BitBucketConfigureOrganizationsPage configureOrganizationsPage =
                jira.getPageBinder().navigateToAndBind(BitBucketConfigureOrganizationsPage.class);
        String repositoryId = configureOrganizationsPage.getRepositoryIdFromRepositoryName(MISSING_COMMITS_REPOSITORY_NAME);

        PostCommitHookCallSimulatingRemoteRestpoint.simulate(jira.getProductInstance().getBaseUrl(), repositoryId);
    }
}
