package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import static org.fest.assertions.api.Assertions.assertThat;
import it.com.atlassian.jira.plugins.dvcs.BitBucketBaseOrgTest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.client.GitHubClient;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubOAuthConfigPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubRegisterOAuthAppPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraAddIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraPageUtils;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.GithubRepositoriesRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.PostCommitHookCallSimulatingRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.plugin.util.zip.FileUnzipper;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Miroslav Stencel
 */
public class MissingCommitsGithubTest extends BitBucketBaseOrgTest<GithubConfigureOrganizationsPage>
{
    private static final String GITHUB_URL = "api.github.com";
    private static final String GITHUB_REPO_OWNER = "dvcsconnectortest";
    private static final String GITHUB_REPO_PASSWORD = PasswordUtil.getPassword("dvcsconnectortest");
    private static final String MISSING_COMMITS_REPOSITORY_NAME = "missingcommitsfixproof";

    private static final String JIRA_PROJECT_NAME_AND_KEY = "MC"; // Missing Commits

    private static final String _1ST_GITHUB_REPO_ZIP_TO_PUSH = "missingCommits/github/git1_2nd_push.zip";
    private static final String _2ND_GITHUB_REPO_ZIP_TO_PUSH = "missingCommits/github/git2_after_merge.zip";

    private static GithubRepositoriesRemoteRestpoint githubRepositoriesREST;
    
    private GithubRegisterOAuthAppPage githubRegisterOAuthAppPage;

    @BeforeClass
    public static void initializeRepositoriesREST()
    {
        GitHubClient gitHubClient = new GitHubClient(GITHUB_URL);
        gitHubClient.setCredentials(GITHUB_REPO_OWNER, GITHUB_REPO_PASSWORD);
        
        githubRepositoriesREST = new GithubRepositoriesRemoteRestpoint(gitHubClient);
    }


    @BeforeMethod
    public void prepareGitRepositoryAndJiraProjectWithIssue()
    {
        removeGitRepositoryAndJiraProject();
        createGitRepositoryAndJiraProjectWithIssue();
    }

    private void removeGitRepositoryAndJiraProject()
    {
        githubRepositoriesREST.removeExistingRepository(MISSING_COMMITS_REPOSITORY_NAME, GITHUB_REPO_OWNER);

        if (JiraPageUtils.projectExists(jira, JIRA_PROJECT_NAME_AND_KEY))
        {
            JiraPageUtils.deleteProject(jira, JIRA_PROJECT_NAME_AND_KEY);
        }
    }

    private void createGitRepositoryAndJiraProjectWithIssue()
    {
        githubRepositoriesREST.createGithubRepository(MISSING_COMMITS_REPOSITORY_NAME);

        JiraPageUtils.createProject(jira, JIRA_PROJECT_NAME_AND_KEY, JIRA_PROJECT_NAME_AND_KEY);
        jira.getPageBinder().navigateToAndBind(JiraAddIssuePage.class).createIssue();
    }

    private void loginToGithubAndSetJiraOAuthCredentials()
    {
        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        jira.getPageBinder().bind(GithubLoginPage.class).doLogin(GITHUB_REPO_OWNER, GITHUB_REPO_PASSWORD);

        jira.getTester().gotoUrl(GithubRegisterOAuthAppPage.PAGE_URL);
        githubRegisterOAuthAppPage = jira.getPageBinder().bind(GithubRegisterOAuthAppPage.class);

        String oauthAppName = "testApp" + System.currentTimeMillis();
        String baseUrl = jira.getProductInstance().getBaseUrl();
        githubRegisterOAuthAppPage.registerApp(oauthAppName, baseUrl, baseUrl);
        String clientID = githubRegisterOAuthAppPage.getClientId().getText();
        String clientSecret = githubRegisterOAuthAppPage.getClientSecret().getText();
        
        GithubOAuthConfigPage oauthConfigPage = jira.getPageBinder().navigateToAndBind(GithubOAuthConfigPage.class);
        oauthConfigPage.setCredentials(clientID, clientSecret);

        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + configureOrganizations.getUrl());
    }

    @Test
    public void commitsIssueTab_ShouldNotMissAnyRelatedCommits() throws Exception
    {
        // repository after the 1st push in following state:
        // +-------------------+------------+--------------------------------------------+
        // | Author            | Commit     | Message                                    |
        // +-------------------+------------+--------------------------------------------+
        // | dvcsconnectortest | 9d08182535 | MC-1 5th commit + 2nd push {user1} [14:26] |
        // | dvcsconnectortest | f6ffeee87f | MC-1 2nd commit + 1st push {user1} [14:18] |
        // | dvcsconnectortest | db26d59a1f | MC-1 1st commit {user1} [14:16]            |
        pushGithubRepository(_1ST_GITHUB_REPO_ZIP_TO_PUSH);

        loginToGithubAndSetJiraOAuthCredentials();
        configureOrganizations.addOrganizationSuccessfully(GITHUB_REPO_OWNER, true);

        assertThat(getCommitsForIssue("MC-1")).hasSize(3);

        // repository after the 2nd push in following state:
        // +-------------------+------------+--------------------------------------------+
        // | Author            | Commit     | Message                                    |
        // +-------------------+------------+--------------------------------------------+
        // | dvcsconnectortest | f59cc8a7b7 | merge + 3rd push {user2} [14:44]           |
        // | dvcsconnectortest | 9d08182535 | MC-1 5th commit + 2nd push {user1} [14:26] |
        // | dvcsconnectortest | cc2ac8c703 | MC-1 4th commit {user2} [14:25]            |
        // | dvcsconnectortest | d5d190c12c | MC-1 3rd commit {user2} [14:24]            |
        // | dvcsconnectortest | f6ffeee87f | MC-1 2nd commit + 1st push {user1} [14:18] |
        // | dvcsconnectortest | db26d59a1f | MC-1 1st commit {user1} [14:16]            |
        pushGithubRepository(_2ND_GITHUB_REPO_ZIP_TO_PUSH);
        
        simulatePostCommitHookCall();
        Thread.sleep(5000); // to catch up with soft sync

        assertThat(getCommitsForIssue("MC-1")).hasSize(5);
    }

    private void pushGithubRepository(String pathToRepoZip) throws IOException, URISyntaxException, InterruptedException
    {
        File extractedRepoDir = extractRepoZipIntoTempDir(pathToRepoZip);

        String gitPushUrl = String.format("https://%1$s:%2$s@github.com/%1$s/%3$s.git", GITHUB_REPO_OWNER,
                GITHUB_REPO_PASSWORD, MISSING_COMMITS_REPOSITORY_NAME);
        
        String gitCommand = getGitCommand();
        
        executeCommand(extractedRepoDir, gitCommand, "remote", "rm", "origin");
        executeCommand(extractedRepoDir, gitCommand, "remote", "add", "origin", gitPushUrl);
        executeCommand(extractedRepoDir, gitCommand, "push", "-u", "origin", "master");

        FileUtils.deleteDirectory(extractedRepoDir);
    }
    
    public static String getGitCommand()
    {
        Process process;
        try
        {
            process = new ProcessBuilder("git").start();
            process.waitFor();
            return "git";
        } catch (Exception e)
        {
            return "/usr/local/git/bin/git";
        }
    }

    
    private void executeCommand(File directory, String... command) throws IOException, InterruptedException
    {
        Process gitPushProcess = new ProcessBuilder(command)
        .directory(directory)
        .start();
        
        
        gitPushProcess.waitFor();
    }
    
    private File extractRepoZipIntoTempDir(String pathToRepoZip) throws IOException, URISyntaxException
    {
        URL repoZipResource = getClass().getClassLoader().getResource(pathToRepoZip);

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "" + System.currentTimeMillis());
        tempDir.mkdir();

        FileUnzipper fileUnzipper = new FileUnzipper(new File(repoZipResource.toURI()), tempDir);
        fileUnzipper.unzip();

        return tempDir;
    }
    
    private void simulatePostCommitHookCall() throws IOException
    {
        GithubConfigureOrganizationsPage configureOrganizationsPage =
                jira.getPageBinder().navigateToAndBind(GithubConfigureOrganizationsPage.class);
        String repositoryId = configureOrganizationsPage.getRepositoryIdFromRepositoryName(MISSING_COMMITS_REPOSITORY_NAME);

        PostCommitHookCallSimulatingRemoteRestpoint.simulate(jira.getProductInstance().getBaseUrl(), repositoryId);
    }


    @Override
    protected Class<GithubConfigureOrganizationsPage> getConfigureOrganizationsPageClass()
    {
        return GithubConfigureOrganizationsPage.class;
    }
}
