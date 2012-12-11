package it.com.atlassian.jira.plugins.dvcs.missingCommits;

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
import com.atlassian.jira.plugins.dvcs.remoterestpoint.GithubRepositoriesRemoteRestpoint;
import com.atlassian.plugin.util.zip.FileUnzipper;

import org.testng.annotations.BeforeClass;

/**
 * @author Miroslav Stencel
 */
public class MissingCommitsGithubTest extends AbstractMissingCommitsTest<GithubConfigureOrganizationsPage>
{
    private static final String GITHUB_URL = "api.github.com";

    private static final String _1ST_GIT_REPO_ZIP_TO_PUSH = "missingCommits/git/git_1st_push.zip";
    private static final String _2ND_GIT_REPO_ZIP_TO_PUSH = "missingCommits/git/git_2nd_push_after_merge.zip";

    private static GithubRepositoriesRemoteRestpoint githubRepositoriesREST;

    @BeforeClass
    public static void initializeGithubRepositoriesREST()
    {
        GitHubClient gitHubClient = new GitHubClient(GITHUB_URL);
        gitHubClient.setCredentials(DVCS_REPO_OWNER, DVCS_REPO_PASSWORD);
        
        githubRepositoriesREST = new GithubRepositoriesRemoteRestpoint(gitHubClient);
    }

    @Override
    void removeRemoteDvcsRepository()
    {
        githubRepositoriesREST.removeExistingRepository(MISSING_COMMITS_REPOSITORY_NAME, DVCS_REPO_OWNER);
    }

    @Override
    void createRemoteDvcsRepository()
    {
        githubRepositoriesREST.createGithubRepository(MISSING_COMMITS_REPOSITORY_NAME);
    }

    @Override
    void loginToDvcsAndSetJiraOAuthCredentials()
    {
        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        jira.getPageBinder().bind(GithubLoginPage.class).doLogin(DVCS_REPO_OWNER, DVCS_REPO_PASSWORD);

        jira.getTester().gotoUrl(GithubRegisterOAuthAppPage.PAGE_URL);
        GithubRegisterOAuthAppPage githubRegisterOAuthAppPage = jira.getPageBinder().bind(GithubRegisterOAuthAppPage.class);

        String oauthAppName = "testApp" + System.currentTimeMillis();
        String baseUrl = jira.getProductInstance().getBaseUrl();
        githubRegisterOAuthAppPage.registerApp(oauthAppName, baseUrl, baseUrl);
        String clientID = githubRegisterOAuthAppPage.getClientId().getText();
        String clientSecret = githubRegisterOAuthAppPage.getClientSecret().getText();
        
        GithubOAuthConfigPage oauthConfigPage = jira.getPageBinder().navigateToAndBind(GithubOAuthConfigPage.class);
        oauthConfigPage.setCredentials(clientID, clientSecret);
    }

    @Override
    void pushToRemoteDvcsRepository(String pathToRepoZip) throws Exception
    {
        File extractedRepoDir = extractRepoZipIntoTempDir(pathToRepoZip);

        String gitPushUrl = String.format("https://%1$s:%2$s@github.com/%1$s/%3$s.git", DVCS_REPO_OWNER,
                DVCS_REPO_PASSWORD, MISSING_COMMITS_REPOSITORY_NAME);

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

    @Override
    String getFirstDvcsZipRepoPathToPush()
    {
        // repository after the 1st push in following state:
        // +------------------+------------+--------------------------------------------+
        // | Author           | Commit     | Message                                    |
        // +------------------+------------+--------------------------------------------+
        // | Miroslav Stencel | 9d08182535 | MC-1 5th commit + 2nd push {user1} [14:26] |
        // | Miroslav Stencel | f6ffeee87f | MC-1 2nd commit + 1st push {user1} [14:18] |
        // | Miroslav Stencel | db26d59a1f | MC-1 1st commit {user1} [14:16]            |
        return _1ST_GIT_REPO_ZIP_TO_PUSH;
    }

    @Override
    String getSecondDvcsZipRepoPathToPush()
    {
        // repository after the 2nd push in following state:
        // +------------------+------------+--------------------------------------------+
        // | Author           | Commit     | Message                                    |
        // +------------------+------------+--------------------------------------------+
        // | Miroslav Stencel | f59cc8a7b7 | merge + 3rd push {user2} [14:44]           |
        // | Miroslav Stencel | 9d08182535 | MC-1 5th commit + 2nd push {user1} [14:26] |
        // | Miroslav Stencel | cc2ac8c703 | MC-1 4th commit {user2} [14:25]            |
        // | Miroslav Stencel | d5d190c12c | MC-1 3rd commit {user2} [14:24]            |
        // | Miroslav Stencel | f6ffeee87f | MC-1 2nd commit + 1st push {user1} [14:18] |
        // | Miroslav Stencel | db26d59a1f | MC-1 1st commit {user1} [14:16]            |
        return _2ND_GIT_REPO_ZIP_TO_PUSH;
    }
    
    @Override
    protected Class<GithubConfigureOrganizationsPage> getConfigureOrganizationsPageClass()
    {
        return GithubConfigureOrganizationsPage.class;
    }
}
