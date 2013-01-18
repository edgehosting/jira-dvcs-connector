package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import java.io.File;

import org.apache.commons.io.FileUtils;

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

import org.testng.annotations.BeforeClass;

/**
 * @author Martin Skurla
 */
public class MissingCommitsBitbucketMercurialTest extends AbstractMissingCommitsTest<BitBucketConfigureOrganizationsPage>
{
    private static final String _1ST_HG_REPO_ZIP_TO_PUSH = "missingCommits/mercurial/hg_1st_push.zip";
    private static final String _2ND_HG_REPO_ZIP_TO_PUSH = "missingCommits/mercurial/hg_2nd_push_after_merge.zip";

    private static BitbucketRepositoriesRemoteRestpoint bitbucketRepositoriesREST;

    private BitbucketIntegratedApplicationsPage bitbucketIntegratedApplicationsPage;

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
        bitbucketRepositoriesREST.createHgRepository(MISSING_COMMITS_REPOSITORY_NAME);
    }

    @Override
    void loginToDvcsAndSetJiraOAuthCredentials()
    {
        jira.getTester().gotoUrl(BitbucketLoginPage.LOGIN_PAGE);
        jira.getPageBinder().bind(BitbucketLoginPage.class).doLogin(DVCS_REPO_OWNER, DVCS_REPO_PASSWORD);

        jira.getTester().gotoUrl("https://bitbucket.org/account/user/dvcsconnectortest/api");
        bitbucketIntegratedApplicationsPage =
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

        String hgPushUrl = String.format("https://%1$s:%2$s@bitbucket.org/%1$s/%3$s", DVCS_REPO_OWNER,
                                                                                      DVCS_REPO_PASSWORD,
                                                                                      MISSING_COMMITS_REPOSITORY_NAME);
        String hgCommand = getHgCommand();
        
        Process hgPushProcess = new ProcessBuilder(hgCommand, "push", hgPushUrl)
                                                  .directory(extractedRepoDir)
                                                  .start();

        hgPushProcess.waitFor();

        FileUtils.deleteDirectory(extractedRepoDir);
    }

    @Override
    String getFirstDvcsZipRepoPathToPush()
    {
        // repository after the 1st push in following state:
        // +---------------+---------+--------------------------------------------+
        // | Author        | Commit  | Message                                    |
        // +---------------+---------+--------------------------------------------+
        // | Martin Skurla | 8b32e32 | MC-1 5th commit + 2nd push {user1} [10:47] |
        // | Martin Skurla | ccdd16b | MC-1 2nd commit + 1st push {user1} [10:38] |
        // | Martin Skurla | 792d8d6 | MC-1 1st commit {user1} [10:37]            |
        return _1ST_HG_REPO_ZIP_TO_PUSH;
    }

    @Override
    String getSecondDvcsZipRepoPathToPush()
    {
        // repository afther the 2nd push in following state:
        // +---------------+---------+--------------------------------------------+
        // | Author        | Commit  | Message                                    |
        // +---------------+---------+--------------------------------------------+
        // | Martin Skurla | 9caa788 | merge + 3rd push {user2} [11:04]           |
        // | Martin Skurla | 066e3b1 | MC-1 4th commit {user2} [10:45]            |
        // | Martin Skurla | 1b05d76 | MC-1 3rd commit {user2} [10:44]            |
        // | Martin Skurla | 8b32e32 | MC-1 5th commit + 2nd push {user1} [10:47] |
        // | Martin Skurla | ccdd16b | MC-1 2nd commit + 1st push {user1} [10:38] |
        // | Martin Skurla | 792d8d6 | MC-1 1st commit {user1} [10:37]            |
        return _2ND_HG_REPO_ZIP_TO_PUSH;
    }
    
    @Override
    protected Class<BitBucketConfigureOrganizationsPage> getConfigureOrganizationsPageClass()
    {
        return BitBucketConfigureOrganizationsPage.class;
    }

    @Override
    void removeOAuth()
    {
        jira.getTester().gotoUrl("https://bitbucket.org/account/user/dvcsconnectortest/api");
        bitbucketIntegratedApplicationsPage.removeLastAdddedConsumer();
    }
}
