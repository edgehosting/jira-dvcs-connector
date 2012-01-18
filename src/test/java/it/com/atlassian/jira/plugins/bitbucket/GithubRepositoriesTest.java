package it.com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.GithubConfigureRepositoriesPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.GithubLoginPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.GithubOAuthConfigPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.GithubRegisterOAuthAppPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.GithubRegisteredOAuthAppsPage;
import com.atlassian.pageobjects.elements.PageElement;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static com.atlassian.jira.plugins.bitbucket.pageobjects.CommitMessageMatcher.withMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;

/**
 * Test to verify behaviour when syncing  github repository.
 */
public class GithubRepositoriesTest extends BitBucketBaseTest
{

    private static final String TEST_REPO_URL = "https://github.com/jirabitbucketconnector/test-project";
    private static final String TEST_PRIVATE_REPO_URL = "https://github.com/dusanhornik/my-private-github-repo";
    private static final String TEST_NOT_EXISTING_REPO_URL = "https://github.com/jirabitbucketconnector/repo-does-not-exist";

    private static String clientID;
    private static String clientSecret;
    private static String oauthAppName;
    private static String oauthAppLink;

    @BeforeClass
    public static void registerAppToGithub()
    {
        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        GithubLoginPage ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogin();

        jira.getTester().gotoUrl(GithubRegisterOAuthAppPage.PAGE_URL);
        GithubRegisterOAuthAppPage registerAppPage = jira.getPageBinder().bind(GithubRegisterOAuthAppPage.class);
        oauthAppName = "testApp" + System.currentTimeMillis();
        String baseUrl = jira.getProductInstance().getBaseUrl();
        registerAppPage.registerApp(oauthAppName, baseUrl, baseUrl);

        jira.getTester().gotoUrl(GithubRegisteredOAuthAppsPage.PAGE_URL);
        GithubRegisteredOAuthAppsPage registeredOAuthAppsPage = jira.getPageBinder().bind(GithubRegisteredOAuthAppsPage.class);
        registeredOAuthAppsPage.parseClientIdAndSecret(oauthAppName);
        clientID = registeredOAuthAppsPage.getClientID();
        clientSecret = registeredOAuthAppsPage.getClientSecret();
        oauthAppLink = registeredOAuthAppsPage.getOauthAppUrl();
        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);

        jira.getTester().gotoUrl(GithubLoginPage.LOGOUT_ACTION_URL);


        GithubOAuthConfigPage oauthConfigPage = jira.gotoLoginPage().loginAsSysAdmin(GithubOAuthConfigPage.class);
        oauthConfigPage.setCredentials(clientID, clientSecret);

        // logout jira
        jira.getTester().getDriver().manage().deleteAllCookies();
    }

    @AfterClass
    public static void deregisterAppToGithub()
    {
        jira.getTester().gotoUrl(GithubLoginPage.LOGOUT_ACTION_URL);

        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        GithubLoginPage ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogin();


        jira.getTester().gotoUrl(oauthAppLink);
        GithubRegisterOAuthAppPage registerAppPage = jira.getPageBinder().bind(GithubRegisterOAuthAppPage.class);
        registerAppPage.deleteOAuthApp();

        jira.getTester().gotoUrl(GithubLoginPage.LOGOUT_ACTION_URL);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Class getPageClass()
    {
        return GithubConfigureRepositoriesPage.class;
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
        assertThat(errorMessage, containsString("Error!The repository url [" + TEST_NOT_EXISTING_REPO_URL + "] is incorrect or the repository is not responding."));
    }

    @Test
    public void addPrivateRepoWithInvalidOAuth()
    {
        configureRepos.deleteAllRepositories();

        goToGithubOAuthConfigPage().setCredentials("xxx", "yyy");

        goToRepositoriesConfigPage();

        configureRepos.addRepoToProjectFailingStep2("QA", TEST_PRIVATE_REPO_URL);
    }

    @Test
    public void addPrivateRepoWithValidOAuth()
    {
        configureRepos.deleteAllRepositories();

        goToGithubOAuthConfigPage().setCredentials(clientID, clientSecret);

        goToRepositoriesConfigPage();

        configureRepos.addPrivateRepoToProjectSuccessfully("QA", TEST_PRIVATE_REPO_URL);

        configureRepos.assertThatSyncMessage(containsString("Sync Finished"));
        configureRepos.assertThatSyncMessage(not(containsString("Sync Failed")));
    }


    @Test
    public void testPostCommitHookAdded() throws Exception
    {
        String baseUrl = jira.getProductInstance().getBaseUrl();

        configureRepos.deleteAllRepositories();
        // add repository
        String repoId = configureRepos.addPublicRepoToProjectAndInstallService("QA",
                TEST_REPO_URL, "jirabitbucketconnector",
                "jirabitbucketconnector1");
        // check that it created postcommit hook

        String githubServiceConfigUrlPath = baseUrl + "/rest/bitbucket/1.0/repository/" + repoId + "/sync";
        String hooksURL = "https://github.com/jirabitbucketconnector/test-project/admin/hooks";
        String hooksPage = getGithubServices(hooksURL, "jirabitbucketconnector", "jirabitbucketconnector1");
        assertThat(hooksPage, containsString(githubServiceConfigUrlPath));
        goToRepositoriesConfigPage();
        // delete repository
        configureRepos.deleteAllRepositories();
        // check that postcommit hook is removed
        hooksPage = getGithubServices(hooksURL, "jirabitbucketconnector",
                "jirabitbucketconnector1");
        assertThat(hooksPage, not(containsString(githubServiceConfigUrlPath)));
    }

    private String getGithubServices(String url, String username, String password) throws Exception
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

        // QA-4
        commitMessages = getCommitsForIssue("QA-4");
        Assert.assertEquals("Expected 1 commit", 1, commitMessages.size());
        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        Assert.assertEquals("Expected 1 statistic", 1, statistics.size());
        Assert.assertTrue("Expected commit resource Added: 1", commitMessage.isAdded(statistics.get(0)));
    }
}
