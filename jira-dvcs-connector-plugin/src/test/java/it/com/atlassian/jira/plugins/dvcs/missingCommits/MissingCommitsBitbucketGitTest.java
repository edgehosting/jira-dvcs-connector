package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import java.io.File;
import java.io.IOException;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketIntegratedApplicationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthConfigPage;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.BitbucketRepositoriesRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.util.ZipUtils;

import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;

/**
 * @author Martin Skurla
 */
public class MissingCommitsBitbucketGitTest extends AbstractMissingCommitsTest<BitBucketConfigureOrganizationsPage>
{
    private static final String _1ST_GIT_REPO_ZIP_TO_PUSH = "missingCommits/git/git_1st_push.zip";
    private static final String _2ND_GIT_REPO_ZIP_TO_PUSH = "missingCommits/git/git_2nd_push_after_merge.zip";

    private static BitbucketRepositoriesRemoteRestpoint bitbucketRepositoriesREST;


    @BeforeClass
    public static void initializeBitbucketRepositoriesREST()
    {
        AuthProvider basicAuthProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                                                                   DVCS_REPO_OWNER,
                                                                   DVCS_REPO_PASSWORD);
        bitbucketRepositoriesREST = new BitbucketRepositoriesRemoteRestpoint(basicAuthProvider.provideRequestor());
    }

    @Override
    void removeRemoteDvcsRepository()
    {
        try
        {
            bitbucketRepositoriesREST.removeExistingRepository(MISSING_COMMITS_REPOSITORY_NAME, DVCS_REPO_OWNER);
        }
        catch (BitbucketRequestException.NotFound_404 e) {} // the repo does not exist
    }

    @Override
    void createRemoteDvcsRepository()
    {
        bitbucketRepositoriesREST.createGitRepository(MISSING_COMMITS_REPOSITORY_NAME);
    }

    @Override
    void loginToDvcsAndSetJiraOAuthCredentials()
    {
        jira.getTester().gotoUrl(BitbucketLoginPage.LOGIN_PAGE);
        jira.getPageBinder().bind(BitbucketLoginPage.class).doLogin(DVCS_REPO_OWNER, DVCS_REPO_PASSWORD);

        jira.getTester().gotoUrl("https://bitbucket.org/account/user/dvcsconnectortest/api");
        BitbucketIntegratedApplicationsPage bitbucketIntegratedApplicationsPage =
                jira.getPageBinder().bind(BitbucketIntegratedApplicationsPage.class);

        BitbucketIntegratedApplicationsPage.OAuthCredentials oauthCredentials =
                bitbucketIntegratedApplicationsPage.addConsumer();

        BitbucketOAuthConfigPage oauthConfigPage = jira.getPageBinder().navigateToAndBind(BitbucketOAuthConfigPage.class);
        oauthConfigPage.setCredentials(oauthCredentials.oauthKey, oauthCredentials.oauthSecret);
    }

    @Override
    void pushToRemoteDvcsRepository(String pathToRepoZip) throws Exception
    {
        File extractedRepoDir = ZipUtils.extractRepoZipIntoTempDir(pathToRepoZip);

        String gitPushUrl = String.format("https://%1$s:%2$s@bitbucket.org/%1$s/%3$s.git", DVCS_REPO_OWNER,
                                                                                           DVCS_REPO_PASSWORD,
                                                                                           MISSING_COMMITS_REPOSITORY_NAME);

        String gitCommand = getGitCommand();

        executeCommand(extractedRepoDir, gitCommand, "remote", "rm", "origin");
        executeCommand(extractedRepoDir, gitCommand, "remote", "add", "origin", gitPushUrl);
        executeCommand(extractedRepoDir, gitCommand, "push", "-u", "origin", "master");

        FileUtils.deleteDirectory(extractedRepoDir);
    }

    private void executeCommand(File workindDirectory, String... command) throws IOException, InterruptedException
    {
        Process process = new ProcessBuilder(command).directory(workindDirectory)
                                                     .start();

        process.waitFor();
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
    protected Class<BitBucketConfigureOrganizationsPage> getConfigureOrganizationsPageClass()
    {
        return BitBucketConfigureOrganizationsPage.class;
    }

    @Override
    void removeOAuth()
    {
        // TODO Remove OAuth consumer
        
    }
}
