package it.com.atlassian.jira.plugins.dvcs.missingCommits;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.ZipFile;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraAddIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraAddProjectPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewProjectsPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import it.com.atlassian.jira.plugins.dvcs.BitBucketBaseOrgTest;

/**
 * @author Martin Skurla
 */
public class MissingCommitsBitbucketMercurialTest extends BitBucketBaseOrgTest
{
    private static final String BITBUCKET_OWNER = "dvcsconnectortest";
    private static final String MISSING_COMMITS_REPOSITORY_NAME = "missingcommitsfixproof";

    private static final String JIRA_PROJECT_NAME_AND_KEY = "MC"; // Missing Commits

    private static final String _1ST_BITBUCKET_REPO_ZIP_TO_PUSH = "missingCommits/bitbucket/hg1 2nd push.zip";
    private static final String _2nd_BITBUCKET_REPO_ZIP_TI_PUSH = "missingCommits/bitbucket/hg2 after merge.zip";


    private static BitbucketRepositoriesRemoteRestpoint bitbucketRepositoriesREST;


    @BeforeClass
    public static void initializeRepositoriesREST()
    {
        AuthProvider basicAuthProvider = new BasicAuthAuthProvider(BitbucketRemoteClient.BITBUCKET_URL,
                                                                   BITBUCKET_OWNER,
                                                                   BITBUCKET_OWNER); // password same as username

        bitbucketRepositoriesREST = new BitbucketRepositoriesRemoteRestpoint(basicAuthProvider.provideRequestor());
    }


    @Before
    public void prepareHgRepositoryAndJiraProjectWithIssue()
    {
        removeHgRepositoryAndJiraProject();
        createHgRepositoryAndJiraProjectWithIssue();
    }

    private void removeHgRepositoryAndJiraProject()
    {
        try
        {
            bitbucketRepositoriesREST.removeExistingRepository(MISSING_COMMITS_REPOSITORY_NAME, BITBUCKET_OWNER);
        }
        catch (BitbucketRequestException.NotFound_404 e) {} // the repo does not exist


        jira.getPageBinder().navigateToAndBind(JiraViewProjectsPage.class).deleteProject(JIRA_PROJECT_NAME_AND_KEY);
    }

    private void createHgRepositoryAndJiraProjectWithIssue()
    {
        bitbucketRepositoriesREST.createHgRepository(MISSING_COMMITS_REPOSITORY_NAME);

        jira.getPageBinder().navigateToAndBind(JiraAddProjectPage.class).createProject(JIRA_PROJECT_NAME_AND_KEY);
        jira.getPageBinder().navigateToAndBind(JiraAddIssuePage.class).createIssue();
    }

    @Test
    public void commitsIssueTab_ShouldNotMissAnyRelatedCommits() throws Exception
    {
        // repository after the 1st push in following state:

        // | Author        | Commit  | Message                                    |
        // +---------------+---------+--------------------------------------------+
        // | Martin Skurla | 8b32e32 | MC-1 5th commit + 2nd push {user1} [10:47] |
        // | Martin Skurla | ccdd16b | MC-1 2nd commit + 1st push {user1} [10:38] |
        // | Martin Skurla | 792d8d6 | MC-1 1st commit {user1} [10:37]            |
        pushBitbucketHgRepository(_1ST_BITBUCKET_REPO_ZIP_TO_PUSH);

        System.out.println("1.) commits for MC-1: " + getCommitsForIssue("MC-1"));

        // repository afther the 2nd push in following state:

        // | Author        | Commit  | Message                                    |
        // +---------------+---------+--------------------------------------------+
        // | Martin Skurla | 9caa788 | merge + 3rd push {user2} [11:04]           |
        // | Martin Skurla | 066e3b1 | MC-1 4th commit {user2} [10:45]            |
        // | Martin Skurla | 1b05d76 | MC-1 3rd commit {user2} [10:44]            |
        // | Martin Skurla | 8b32e32 | MC-1 5th commit + 2nd push {user1} [10:47] |
        // | Martin Skurla | ccdd16b | MC-1 2nd commit + 1st push {user1} [10:38] |
        // | Martin Skurla | 792d8d6 | MC-1 1st commit {user1} [10:37]            |
        pushBitbucketHgRepository(_2nd_BITBUCKET_REPO_ZIP_TI_PUSH);

        System.out.println("2.) commits for MC-1: " + getCommitsForIssue("MC-1"));
    }

    private void pushBitbucketHgRepository(String pathToRepoZip) throws IOException, URISyntaxException, InterruptedException
    {
        File extractedRepoDir = extractRepoZipIntoTempDir(pathToRepoZip);

        String hgPushUrl = String.format("https://%1$s:%1$s@bitbucket.org/%1$s/%2$s", BITBUCKET_OWNER,
                                                                                      MISSING_COMMITS_REPOSITORY_NAME);

        Process hgPushProcess = new ProcessBuilder("hg", "push", hgPushUrl)
                                                  .directory(extractedRepoDir)
                                                  .start();

        hgPushProcess.waitFor();
    }

    private File extractRepoZipIntoTempDir(String pathToRepoZip) throws IOException, URISyntaxException
    {
        URL repoZipResource = getClass().getClassLoader().getResource(pathToRepoZip);

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "" + System.currentTimeMillis());
        tempDir.mkdir();

        ZipUtils.unzipFileIntoDirectory(new ZipFile(new File(repoZipResource.toURI())), tempDir);

        return tempDir;
    }


    @Override
    protected Class getPageClass()
    {
        return BitBucketConfigureOrganizationsPage.class;
    }
}
