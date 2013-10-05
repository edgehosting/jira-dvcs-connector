package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.GithubRepositoriesRemoteRestpoint;
import com.atlassian.plugin.util.zip.FileUnzipper;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthPage;
import org.apache.commons.io.FileUtils;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

/**
 * @author Miroslav Stencel
 */
@Test
public class MissingCommitsGithubTest extends AbstractMissingCommitsTest<GithubConfigureOrganizationsPage>
{
    private static final String GITHUB_URL = "api.github.com";
    private static final String USER_AGENT = "DVCS Connector Test/X.x";

    private static final String _1ST_GIT_REPO_ZIP_TO_PUSH = "missingCommits/git/git_1st_push.zip";
    private static final String _2ND_GIT_REPO_ZIP_TO_PUSH = "missingCommits/git/git_2nd_push_after_merge.zip";

    private static GithubRepositoriesRemoteRestpoint githubRepositoriesREST;

    @BeforeClass
    public static void initializeGithubRepositoriesREST()
    {
        GitHubClient gitHubClient = new GitHubClient(GITHUB_URL);
        gitHubClient.setUserAgent(USER_AGENT);
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
    OAuth loginToDvcsAndGetJiraOAuthCredentials()
    {
        // log in to github
        new MagicVisitor(jira).visit(GithubLoginPage.class).doLogin(DVCS_REPO_OWNER, DVCS_REPO_PASSWORD);
        // setup up OAuth from github
        return new MagicVisitor(jira).visit(GithubOAuthPage.class).addConsumer(jira.getProductInstance().getBaseUrl());
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

    @Override
    void removeOAuth()
    {
        // remove OAuth in github
        new MagicVisitor(jira).visit(oAuth.applicationId, GithubOAuthPage.class).removeConsumer();
        // log out from github
        new MagicVisitor(jira).visit(GithubLoginPage.class).doLogout();
    }
}
