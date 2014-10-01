package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.BitbucketRepositoriesRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.util.ZipUtils;
import org.apache.commons.io.FileUtils;
import org.testng.annotations.BeforeClass;

import java.io.File;

/**
 * @author Martin Skurla
 */
public class MissingCommitsBitbucketMercurialTest extends AbstractMissingCommitsTest<BitBucketConfigureOrganizationsPage>
{
    private static final String _1ST_HG_REPO_ZIP_TO_PUSH = "missingCommits/mercurial/hg_1st_push.zip";
    private static final String _2ND_HG_REPO_ZIP_TO_PUSH = "missingCommits/mercurial/hg_2nd_push_after_merge.zip";

    private static BitbucketRepositoriesRemoteRestpoint bitbucketRepositoriesREST;

    @BeforeClass
    public static void initializeBitbucketRepositoriesREST()
    {
        HttpClientProvider httpClientProvider = new HttpClientProvider();
        httpClientProvider.setUserAgent(BitbucketRemoteClient.TEST_USER_AGENT);

        AuthProvider basicAuthProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                                                                   DVCS_REPO_OWNER,
                                                                   DVCS_REPO_PASSWORD,
                                                                   httpClientProvider);
        bitbucketRepositoriesREST = new BitbucketRepositoriesRemoteRestpoint(basicAuthProvider.provideRequestor());
    }

    @Override
    void removeOldDvcsRepository()
    {
        try
        {
            bitbucketRepositoriesREST.removeExistingRepository(MISSING_COMMITS_REPOSITORY_NAME_PREFIX, DVCS_REPO_OWNER);
        }
        catch (BitbucketRequestException.NotFound_404 e) {} // the repo does not exist
    }

    @Override
    void removeRemoteDvcsRepository()
    {
        removeRepository(getMissingCommitsRepositoryName());

        for ( BitbucketRepository repository : bitbucketRepositoriesREST.getAllRepositories(DVCS_REPO_OWNER))
        {
            if (timestampNameTestResource.isExpired(repository.getName()))
            {
                removeRepository(repository.getName());
            }
        }
    }

    private void removeRepository(String name)
    {
        try
        {
            bitbucketRepositoriesREST.removeExistingRepository(name, DVCS_REPO_OWNER);
        }
        catch (BitbucketRequestException.NotFound_404 e) {} // the repo does not exist
    }

    @Override
    void createRemoteDvcsRepository()
    {
        bitbucketRepositoriesREST.createHgRepository(getMissingCommitsRepositoryName());
    }

    @Override
    OAuth loginToDvcsAndGetJiraOAuthCredentials()
    {
        // log in to Bitbucket
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogin(DVCS_REPO_OWNER, DVCS_REPO_PASSWORD);
        // setup up OAuth from bitbucket
        return new MagicVisitor(jira).visit(BitbucketOAuthPage.class,DVCS_REPO_OWNER).addConsumer();
    }

    @Override
    void pushToRemoteDvcsRepository(String pathToRepoZip) throws Exception
    {
        File extractedRepoDir = ZipUtils.extractRepoZipIntoTempDir(pathToRepoZip);

        String hgPushUrl = String.format("https://%1$s:%2$s@bitbucket.org/%1$s/%3$s", DVCS_REPO_OWNER,
                                                                                      DVCS_REPO_PASSWORD,
                                                                                      getMissingCommitsRepositoryName());
        executeCommand(extractedRepoDir, getHgCommand(), "push", hgPushUrl);
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
        if (oAuth != null)
        {
            // remove OAuth in bitbucket
            new MagicVisitor(jira).visit(BitbucketOAuthPage.class, DVCS_REPO_OWNER).removeConsumer(oAuth.applicationId);
        }
        // log out from bitbucket
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogout();
    }

    @Override
    protected AccountsPageAccount.AccountType getAccountType()
    {
        return AccountsPageAccount.AccountType.BITBUCKET;
    }
}
