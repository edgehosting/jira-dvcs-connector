package it.com.atlassian.jira.plugins.dvcs;

import org.junit.Test;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;

/**
 * Test to verify behaviour when syncing bitbucket repository..
 */
public class BitbucketOrganzationsTest extends BitBucketBaseOrgTest
{
    private static final String TEST_ORG_URL = "https://bitbucket.org";
//    private static final String TEST_PUBLIC_REPO_URL = "https://bitbucket.org/jirabitbucketconnector/public-hg-repo";
//    private static final String TEST_PRIVATE_REPO_URL = "https://bitbucket.org/jirabitbucketconnector/private-hg-repo";
    private static final String TEST_NOT_EXISTING_REPO_URL = "https://bitbucket.org/jirabitbucketconnector/repo-does-not-exist";
    private static final String REPO_ADMIN_LOGIN = "jirabitbucketconnector";
    private static final String REPO_ADMIN_PASSWORD = "jirabitbucketconnector1";

    @Override
    protected Class<BitBucketConfigureOrganizationsPage> getPageClass()
    {
        return BitBucketConfigureOrganizationsPage.class;
    }


    @Test
    public void addPublicOrganization()
    {
        configureOrganizations.addOrganizationSuccessfully(TEST_ORG_URL);
    }

    @Test
    public void addPrivateRepo()
    {
       // configureOrganizations.addOrganizationSuccessfully("QA", TEST_PRIVATE_REPO_URL);
    }

  /*  @Test
    public void addRepoThatDoesNotExist()
    {
        configureOrganizations.addRepoToProjectFailingStep1("QA", TEST_NOT_EXISTING_REPO_URL);

        String errorMessage = configureOrganizations.getErrorStatusMessage();
        assertThat(errorMessage, containsString("The repository url [" + TEST_NOT_EXISTING_REPO_URL + "] is incorrect or the repository is not responding."));
        configureOrganizations.clearForm();
    }

    @Test
    public void addRepoAppearsOnList()
    {
        configureOrganizations.addOrganizationSuccessfully(TEST_PUBLIC_REPO_URL);
        assertThat(configureOrganizations.getRepositories().size(), equalTo(1));
    }

    @Test
    public void addRepoCommitsAppearOnIssues()
    {
        ensureOrganizationPresent("QA", TEST_PUBLIC_REPO_URL);

        assertThat(getCommitsForIssue("QA-2"),
                hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
        assertThat(getCommitsForIssue("QA-3"),
                hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
    }

    @Test
    public void testPostCommitHookAdded() throws Exception
    {
        String servicesConfig;
        String baseUrl = jira.getProductInstance().getBaseUrl();

        // add repository
        String repoId = configureOrganizations.addPublicRepoToProjectAndInstallService("QA",
                TEST_PUBLIC_REPO_URL, REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);
        // check that it created postcommit hook
        String syncUrl = baseUrl + "/rest/bitbucket/1.0/repository/" + repoId + "/sync";
        String bitbucketServiceConfigUrl = "https://api.bitbucket.org/1.0/repositories/jirabitbucketconnector/public-hg-repo/services";
        servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);
        assertThat(servicesConfig, containsString(syncUrl));
        // delete repository
        configureOrganizations.deleteAllRepositories();
        // check that postcommit hook is removed
        servicesConfig = getBitbucketServices(bitbucketServiceConfigUrl, REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);
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
        configureOrganizations.addOrganizationSuccessfully(TEST_PUBLIC_REPO_URL);
        assertThat(configureOrganizations.getRepositories().size(), equalTo(1));

        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-2");
        assertThat(commitMessages, hasItem(withMessageLinks("QA-3")));
    }

    @Test
    public void testCommitStatistics()
    {
        configureOrganizations.addOrganizationSuccessfully(TEST_PUBLIC_REPO_URL);

        // QA-2
        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-2");
        Assert.assertEquals("Expected 1 commit", 1, commitMessages.size());
        BitBucketCommitEntry commitMessage = commitMessages.get(0);
        List<PageElement> statistics = commitMessage.getStatistics();
        Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
        Assert.assertTrue("Expected Added", commitMessage.isAdded(statistics.get(0)));

        // QA-3
        commitMessages = getCommitsForIssue("QA-3");
        Assert.assertEquals("Expected 2 commits", 2, commitMessages.size());
        // commit 1
        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
        Assert.assertTrue("Expected Added", commitMessage.isAdded(statistics.get(0)));
        // commit 2
        commitMessage = commitMessages.get(1);
        statistics = commitMessage.getStatistics();
        Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
        Assert.assertEquals("Expected Additions: 1", commitMessage.getAdditions(statistics.get(0)), "+3");
        Assert.assertEquals("Expected Deletions: -", commitMessage.getDeletions(statistics.get(0)), "-");
    }*/
   
   
}
