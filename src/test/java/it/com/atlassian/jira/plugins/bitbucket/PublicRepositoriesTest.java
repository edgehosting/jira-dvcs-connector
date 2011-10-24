package it.com.atlassian.jira.plugins.bitbucket;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static com.atlassian.jira.plugins.bitbucket.pageobjects.CommitMessageMatcher.withMessage;


/**
 * Test to verify behaviour when syncing public bitbucket repos.
 */
public class PublicRepositoriesTest extends BitBucketBaseTest
{
    private static final String TEST_REPO_URL = "https://bitbucket.org/farmas/testrepo-qa";
    private static final String TEST_PRIVATE_REPO_URL = "https://bitbucket.org/farmas/privatetestrepo-qa-tst";

    @Test
    public void addRepoAppearsOnList()
    {
        configureRepos.deleteAllRepositories();
        configureRepos.addPublicRepoToProject("QA", TEST_REPO_URL);
        assertThat(configureRepos.getRepositories().size(), equalTo(1));
    }

    @Test
    public void addRepoCommitsAppearOnIssues()
    {
        ensureRepositoryPresent("QA", TEST_REPO_URL);

	    assertThat(getCommitsForIssue("QA-2"), hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
        assertThat(getCommitsForIssue("QA-3"), hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
    }

    @Test
    public void addRepoThatDoesNotExist()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addPublicRepoToProject("QA", "https://bitbucket.org/farmas/repo-does-not-exist");
        
        String syncStatusMessage = configureRepos.getSyncStatusMessage();
        assertThat(syncStatusMessage, containsString("Sync Failed"));
        
        // BBC-60
        // assertThat(syncStatusMessage, containsString("Bitbucket repository can't be found or incorrect credentials."));
    }

    @Test
    public void addPrivateRepoAsPublic()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addPublicRepoToProject("QA", TEST_PRIVATE_REPO_URL);

        String syncStatusMessage = configureRepos.getSyncStatusMessage();
        assertThat(syncStatusMessage, containsString("Sync Failed"));

        // BBC-60
        // assertThat(syncStatusMessage, containsString("Bitbucket repository can't be found or incorrect credentials."));
    }
}
