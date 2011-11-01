package it.com.atlassian.jira.plugins.bitbucket;

import static com.atlassian.jira.plugins.bitbucket.pageobjects.CommitMessageMatcher.withMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

import org.junit.Test;

/**
 * Test to verify behaviour when syncing public bitbucket repos.
 */
public class PublicRepositoriesTest extends BitBucketBaseTest
{
    private static final String TEST_REPO_URL = "https://bitbucket.org/farmas/testrepo-qa";
    private static final String TEST_PRIVATE_REPO_URL = "https://bitbucket.org/farmas/privatetestrepo-qa-tst";
    private final BitbucketApi bitbucketApi = new BitbucketApi();

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

        assertThat(getCommitsForIssue("QA-2"),
                hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
        assertThat(getCommitsForIssue("QA-3"),
                hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
    }

    @Test
    public void addRepoThatDoesNotExist()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addPublicRepoToProject("QA", "https://bitbucket.org/farmas/repo-does-not-exist");

        String syncStatusMessage = configureRepos.getSyncStatusMessage();
        assertThat(syncStatusMessage, containsString("Sync Failed"));
    }

    @Test
    public void addPrivateRepoAsPublic()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addPublicRepoToProject("QA", TEST_PRIVATE_REPO_URL);

        String syncStatusMessage = configureRepos.getSyncStatusMessage();
        assertThat(syncStatusMessage, containsString("Sync Failed"));
    }

    public void testGitRepository()
    {
        // TODO
    }

    @Test
    public void testPostCommitHookAdded() throws Exception
    {
        String servicesConfig;
        String baseUrl = jira.getProductInstance().getBaseUrl();

        configureRepos.deleteAllRepositories();
        // add repository
        String repoId = configureRepos.addPublicRepoToProjectAndInstallService("QA",
                "https://bitbucket.org/jirabitbucketconnector/public-hg-repo", "jirabitbucketconnector",
                "jirabitbucketconnector");
        // check that it created postcommit hook
        String syncUrl = baseUrl + "/rest/bitbucket/1.0/repository/" + repoId + "/sync";
        String bitbucketServiceConfigUrl = "https://api.bitbucket.org/1.0/repositories/jirabitbucketconnector/public-hg-repo/services";
        servicesConfig = bitbucketApi.getServices(bitbucketServiceConfigUrl, "jirabitbucketconnector",
                "jirabitbucketconnector");
        assertThat(servicesConfig, containsString(syncUrl));
        // delete repository
        configureRepos.deleteAllRepositories();
        // check that postcommit hook is removed
        servicesConfig = bitbucketApi.getServices(bitbucketServiceConfigUrl, "jirabitbucketconnector",
                "jirabitbucketconnector");
        assertThat(servicesConfig, not(containsString(syncUrl)));
    }

    public void testSyncFromPostCommit()
    {
        // TODO
    }
}
