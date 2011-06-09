package it.com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.JiraViewIssuePage;
import org.junit.Test;

import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Test to verify behaviour when syncing public bitbucket repos.
 */
public class PublicRepositoriesTest extends BitBucketBaseTest
{
    private static final String TEST_REPO_URL = "https://bitbucket.org/farmas/testrepo-qa";

    @Test
    public void addPublicRepo_VerifyAppearsInList()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addPublicRepoToProject("QA", TEST_REPO_URL);

        assertEquals(1, configureRepos.getRepositories().size());
    }

    @Test
    public void addPublicRepo_VerifyCommitsOnIssues()
    {
        if(configureRepos.isRepositoryPresent("QA", TEST_REPO_URL + "/default") == false)
        {
            configureRepos.addPublicRepoToProject("QA", TEST_REPO_URL);
        }

        List<BitBucketCommitEntry> commitList = jira.visit(JiraViewIssuePage.class, "QA-1")
                                                          .openBitBucketPanel()
                                                          .waitForMessages();

        assertTrue("Expected at least 1 commit entry", commitList.size() > 1);

        //verify message of first commit
        assertEquals("BB modified 10 files to QA-1 from TestRepo-QA", commitList.get(commitList.size()-1).getCommitMessage());

    }
}
