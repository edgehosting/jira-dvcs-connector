package it.com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.BitBucketConfigureRepositoriesPage;
import com.atlassian.pageobjects.elements.PageElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.atlassian.jira.plugins.bitbucket.pageobjects.CommitMessageLinksMatcher.withMessageLinks;
import static com.atlassian.jira.plugins.bitbucket.pageobjects.CommitMessageMatcher.withMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * Test to verify behaviour when syncing bitbucket repository..
 */
public class BitbucketRepositoriesTest extends BitBucketBaseTest
{
    private static final String TEST_REPO_URL = "https://bitbucket.org/farmas/testrepo-qa";
    private static final String TEST_PRIVATE_REPO_URL = "https://bitbucket.org/jirabitbucketconnector/private-hg-repo";
    private static final String TEST_NOT_EXISTING_REPO_URL = "https://bitbucket.org/jirabitbucketconnector/repo-does-not-exist";

    @Override
    protected Class<BitBucketConfigureRepositoriesPage> getPageClass()
    {
        return BitBucketConfigureRepositoriesPage.class;
    }

    @Test
    public void addRepoAppearsOnList()
    {
        configureRepos.deleteAllRepositories();
        configureRepos.addPublicRepoToProjectSuccessfully("QA", TEST_REPO_URL);
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

        configureRepos.addRepoToProjectFailingStep1("QA", TEST_NOT_EXISTING_REPO_URL);

        String errorMessage = configureRepos.getErrorStatusMessage();
        assertThat(errorMessage, containsString("The repository url [" + TEST_NOT_EXISTING_REPO_URL + "] is incorrect or the repository is not responding."));
    }

    @Test
    public void addPrivateRepoAsPublic()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addPrivateRepoToProjectSuccessfully("QA", TEST_PRIVATE_REPO_URL);

        configureRepos.assertThatSyncMessage(containsString("Sync Finished"));
    }

    @Test
    public void addPrivateRepo()
    {
        configureRepos.deleteAllRepositories();

        configureRepos.addPrivateRepoToProjectSuccessfully("QA", TEST_PRIVATE_REPO_URL);

        configureRepos.assertThatSyncMessage(containsString("Sync Finished"));
        configureRepos.assertThatSyncMessage(not(containsString("Sync Failed")));
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
        servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, "jirabitbucketconnector",
                "jirabitbucketconnector");
        assertThat(servicesConfig, containsString(syncUrl));
        // delete repository
        configureRepos.deleteAllRepositories();
        // check that postcommit hook is removed
        servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, "jirabitbucketconnector",
                "jirabitbucketconnector");
        assertThat(servicesConfig, not(containsString(syncUrl)));
    }

    private String getBitbucketServices(String url, String username, String password) throws Exception
    {
        HttpClient httpClient = new HttpClient();
        HttpMethod method = new GetMethod(url);

        AuthScope authScope = new AuthScope(method.getURI().getHost(), AuthScope.ANY_PORT, null, AuthScope.ANY_SCHEME);
        httpClient.getParams().setAuthenticationPreemptive(true);
        httpClient.getState().setCredentials(authScope, new UsernamePasswordCredentials(username, password));

        httpClient.executeMethod(method);
        return method.getResponseBodyAsString();
    }

    @Test
    public void testIssueLinkerCommentFormatting()
    {
        configureRepos.deleteAllRepositories();
        configureRepos.addPublicRepoToProjectSuccessfully("QA", TEST_REPO_URL);
        assertThat(configureRepos.getRepositories().size(), equalTo(1));

        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-2");
        assertThat(commitMessages, hasItem(withMessageLinks("QA-2", "QA-3")));
    }

    @Test
    public void testCommitStatistics()
    {
        configureRepos.deleteAllRepositories();
        configureRepos.addPublicRepoToProjectSuccessfully("QA", TEST_REPO_URL);

        // QA-2
        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-2");
        Assert.assertEquals("Expected 1 commit", 1, commitMessages.size());
        BitBucketCommitEntry commitMessage = commitMessages.get(0);
        List<PageElement> statistics = commitMessage.getStatistics();
        Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
        Assert.assertEquals("Expected Additions: 1", commitMessage.getAdditions(statistics.get(0)), "+1");
        Assert.assertEquals("Expected Deletions: -", commitMessage.getDeletions(statistics.get(0)), "-");

        // QA-3
        commitMessages = getCommitsForIssue("QA-3");
        Assert.assertEquals("Expected 2 commits", 2, commitMessages.size());
        // commit 1
        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
        Assert.assertEquals("Expected Additions: 1", commitMessage.getAdditions(statistics.get(0)), "+1");
        Assert.assertEquals("Expected Deletions: -", commitMessage.getDeletions(statistics.get(0)), "-");
        // commit 2
        commitMessage = commitMessages.get(1);
        statistics = commitMessage.getStatistics();
        Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
        Assert.assertEquals("Expected Additions: 1", commitMessage.getAdditions(statistics.get(0)), "+1");
        Assert.assertEquals("Expected Deletions: -", commitMessage.getDeletions(statistics.get(0)), "-");
    }
}
