package it.com.atlassian.jira.plugins.bitbucket;

import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static it.com.atlassian.jira.plugins.bitbucket.CommitMessageMatcher.withMessage;


/**
 * Test to verify behaviour when syncing public bitbucket repos.
 */
public class PublicRepositoriesTest extends BitBucketBaseTest
{
    private static final String TEST_REPO_URL = "https://bitbucket.org/farmas/testrepo-qa";

    @Test
    public void addingRepoAppearsOnList()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addPublicRepoToProject("QA", TEST_REPO_URL);

        assertThat(configureRepos.getRepositories().size(), equalTo(1));
    }

    @Test
    public void addingRepoCommitsAppearOnIssues()
    {
        ensureRepositoryPresent("QA", TEST_REPO_URL);

        assertThat(getCommitsForIssue("QA-2"), hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
        assertThat(getCommitsForIssue("QA-3"), hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
    }
}
