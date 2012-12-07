package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import static org.fest.assertions.api.Assertions.assertThat;
import it.com.atlassian.jira.plugins.dvcs.BitBucketBaseOrgTest;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketIntegratedApplicationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthConfigPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraPageUtils;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.BitbucketRepositoriesRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.PostCommitHookCallSimulatingRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.plugin.util.zip.FileUnzipper;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Martin Skurla
 */
public class MissingCommitsBitbucketMercurialTest extends BitBucketBaseOrgTest<BitBucketConfigureOrganizationsPage>
{
    private static final String BITBUCKET_REPO_OWNER = "dvcsconnectortest";
    private static final String BITBUCKET_REPO_PASSWORD = PasswordUtil.getPassword("dvcsconnectortest");
    private static final String MISSING_COMMITS_REPOSITORY_NAME = "missingcommitsfixproof";

    private static final String JIRA_PROJECT_NAME_AND_KEY = "MC"; // Missing Commits

    private static final String _1ST_HG_REPO_ZIP_TO_PUSH = "missingCommits/mercurial/hg_1st_push.zip";
    private static final String _2ND_HG_REPO_ZIP_TO_PUSH = "missingCommits/mercurial/hg_2nd_push_after_merge.zip";

    private static BitbucketRepositoriesRemoteRestpoint bitbucketRepositoriesREST;


    @BeforeClass
    public static void initializeRepositoriesREST()
    {
        AuthProvider basicAuthProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                                                                   BITBUCKET_REPO_OWNER,
                                                                   BITBUCKET_REPO_PASSWORD);
        bitbucketRepositoriesREST = new BitbucketRepositoriesRemoteRestpoint(basicAuthProvider.provideRequestor());
    }


    @BeforeMethod
    public void prepareHgRepositoryAndJiraProjectWithIssue()
    {
        removeHgRepositoryAndJiraProject();
        createHgRepositoryAndJiraProjectWithIssue();
    }

    private void removeHgRepositoryAndJiraProject()
    {
        try
        {
            bitbucketRepositoriesREST.removeExistingRepository(MISSING_COMMITS_REPOSITORY_NAME, BITBUCKET_REPO_OWNER);
        }
        catch (BitbucketRequestException.NotFound_404 e) {} // the repo does not exist

        if (JiraPageUtils.projectExists(jira, JIRA_PROJECT_NAME_AND_KEY))
        {
            JiraPageUtils.deleteProject(jira, JIRA_PROJECT_NAME_AND_KEY);
        }
    }

    private void createHgRepositoryAndJiraProjectWithIssue()
    {
        bitbucketRepositoriesREST.createHgRepository(MISSING_COMMITS_REPOSITORY_NAME);

        JiraPageUtils.createProject(jira, JIRA_PROJECT_NAME_AND_KEY, JIRA_PROJECT_NAME_AND_KEY);
        JiraPageUtils.createIssue(jira, JIRA_PROJECT_NAME_AND_KEY);
    }

    private void loginToBitbucketAndSetJiraOAuthCredentials()
    {
        jira.getTester().gotoUrl(BitbucketLoginPage.LOGIN_PAGE);
        jira.getPageBinder().bind(BitbucketLoginPage.class).doLogin(BITBUCKET_REPO_OWNER, BITBUCKET_REPO_PASSWORD);

        jira.getTester().gotoUrl("https://bitbucket.org/account/user/dvcsconnectortest/api");
        BitbucketIntegratedApplicationsPage bitbucketIntegratedApplicationsPage =
                jira.getPageBinder().bind(BitbucketIntegratedApplicationsPage.class);

        BitbucketIntegratedApplicationsPage.OAuthCredentials oauthCredentials =
                bitbucketIntegratedApplicationsPage.addConsumer();

        BitbucketOAuthConfigPage oauthConfigPage = jira.getPageBinder().navigateToAndBind(BitbucketOAuthConfigPage.class);
        oauthConfigPage.setCredentials(oauthCredentials.oauthKey, oauthCredentials.oauthSecret);

        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + configureOrganizations.getUrl());
    }

    @Test
    public void commitsIssueTab_ShouldNotMissAnyRelatedCommits() throws Exception
    {
        // repository after the 1st push in following state:
        // +---------------+---------+--------------------------------------------+
        // | Author        | Commit  | Message                                    |
        // +---------------+---------+--------------------------------------------+
        // | Martin Skurla | 8b32e32 | MC-1 5th commit + 2nd push {user1} [10:47] |
        // | Martin Skurla | ccdd16b | MC-1 2nd commit + 1st push {user1} [10:38] |
        // | Martin Skurla | 792d8d6 | MC-1 1st commit {user1} [10:37]            |
        pushBitbucketHgRepository(_1ST_HG_REPO_ZIP_TO_PUSH);

        loginToBitbucketAndSetJiraOAuthCredentials();
        configureOrganizations.addOrganizationSuccessfully(BITBUCKET_REPO_OWNER, true);

        assertThat(getCommitsForIssue("MC-1", 3)).hasSize(3);

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
        pushBitbucketHgRepository(_2ND_HG_REPO_ZIP_TO_PUSH);
        
        simulatePostCommitHookCall();
        Thread.sleep(5000); // to catch up with soft sync

        assertThat(getCommitsForIssue("MC-1", 5)).hasSize(5);
    }

    private void pushBitbucketHgRepository(String pathToRepoZip) throws IOException, URISyntaxException, InterruptedException
    {
        File extractedRepoDir = extractRepoZipIntoTempDir(pathToRepoZip);

        String hgPushUrl = String.format("https://%1$s:%2$s@bitbucket.org/%1$s/%3$s", BITBUCKET_REPO_OWNER,
                                                                                      BITBUCKET_REPO_PASSWORD,
                                                                                      MISSING_COMMITS_REPOSITORY_NAME);

        Process hgPushProcess = new ProcessBuilder("hg", "push", hgPushUrl)
                                                  .directory(extractedRepoDir)
                                                  .start();

        hgPushProcess.waitFor();

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
    
    private void simulatePostCommitHookCall() throws IOException
    {
        BitBucketConfigureOrganizationsPage configureOrganizationsPage =
                jira.getPageBinder().navigateToAndBind(BitBucketConfigureOrganizationsPage.class);
        String repositoryId = configureOrganizationsPage.getRepositoryIdFromRepositoryName(MISSING_COMMITS_REPOSITORY_NAME);

        PostCommitHookCallSimulatingRemoteRestpoint.simulate(jira.getProductInstance().getBaseUrl(), repositoryId);
    }


    @Override
    protected Class<BitBucketConfigureOrganizationsPage> getConfigureOrganizationsPageClass()
    {
        return BitBucketConfigureOrganizationsPage.class;
    }
}
