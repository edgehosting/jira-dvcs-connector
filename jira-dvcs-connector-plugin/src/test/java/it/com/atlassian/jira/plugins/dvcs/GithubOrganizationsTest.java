package it.com.atlassian.jira.plugins.dvcs;

import static com.atlassian.jira.plugins.dvcs.pageobjects.CommitMessageMatcher.withMessage;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubOAuthConfigPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubRegisterOAuthAppPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubRegisteredOAuthAppsPage;
import com.atlassian.jira.plugins.dvcs.util.HttpSenderUtils;
import com.atlassian.jira.util.json.JSONArray;
import com.atlassian.jira.util.json.JSONException;
import com.atlassian.jira.util.json.JSONObject;
import com.atlassian.pageobjects.elements.PageElement;

/**
 * Test to verify behaviour when syncing  github repository.
 */
public class GithubOrganizationsTest extends BitBucketBaseOrgTest
{

    private static final String TEST_URL = "https://github.com";
    private static final String TEST_ORGANIZATION = "jirabitbucketconnector";
    private static final String TEST_NOT_EXISTING_URL = "https://privategithub.com/myaccount";
    private static final String REPO_ADMIN_LOGIN = "jirabitbucketconnector";
    private static final String REPO_ADMIN_PASSWORD = "jirabitbucketconnector1";

    private static String clientID;
    private static String clientSecret;
    private static String oauthAppLink;

    @BeforeClass
    public static void registerAppToGithub()
    {
        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        GithubLoginPage ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogin();

        // find out secrets
        jira.getTester().gotoUrl(GithubRegisterOAuthAppPage.PAGE_URL);
        GithubRegisterOAuthAppPage registerAppPage = jira.getPageBinder().bind(GithubRegisterOAuthAppPage.class);
        String oauthAppName = "testApp" + System.currentTimeMillis();
        String baseUrl = jira.getProductInstance().getBaseUrl();
        registerAppPage.registerApp(oauthAppName, baseUrl, baseUrl);
        clientID = registerAppPage.getClientId().getText();
        clientSecret = registerAppPage.getClientSecret().getText();

        // find out app URL
        jira.getTester().gotoUrl(GithubRegisteredOAuthAppsPage.PAGE_URL);
        GithubRegisteredOAuthAppsPage registeredOAuthAppsPage = jira.getPageBinder().bind(GithubRegisteredOAuthAppsPage.class);
        registeredOAuthAppsPage.parseClientIdAndSecret(oauthAppName);
        oauthAppLink = registeredOAuthAppsPage.getOauthAppUrl();
        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        jira.getTester().gotoUrl(GithubLoginPage.LOGOUT_ACTION_URL);


        GithubOAuthConfigPage oauthConfigPage = jira.getPageBinder().navigateToAndBind(AnotherLoginPage.class).loginAsSysAdmin(GithubOAuthConfigPage.class);
        oauthConfigPage.setCredentials(clientID, clientSecret);

        // logout jira
        jira.getTester().getDriver().manage().deleteAllCookies();
    }

    @AfterClass
    public static void deregisterAppToGithub()
    {
       /* jira.getTester().gotoUrl(GithubLoginPage.LOGOUT_ACTION_URL);

        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        GithubLoginPage ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogin();
*/

        jira.getTester().gotoUrl(oauthAppLink);

    	try
		{
			jira.getTester().getDriver().switchTo().alert().accept();
		} catch (Exception e)
		{
			// nop, probably no leave page alert
		}

        GithubRegisterOAuthAppPage registerAppPage = jira.getPageBinder().bind(GithubRegisterOAuthAppPage.class);
        registerAppPage.deleteOAuthApp();

        jira.getTester().gotoUrl(GithubLoginPage.LOGOUT_ACTION_URL);
    }

    @Before
    public void removeExistingPostCommitHooks()
    {
        String[] githubRepositories = { "repo1", "test-project" };
        for (String githubRepositoryId : githubRepositories)
        {
            Set<String> extractedGithubHookIds = extractGithubHookIdsForRepositoryToRemove(githubRepositoryId);
            for (String extractedGithubHookId : extractedGithubHookIds)
            {
                removePostCommitHook(githubRepositoryId, extractedGithubHookId);
            }
        }
    }

    @After
    public void deleteRepositoriesAfterTest()
    {
        goToConfigPage();
        configureOrganizations.deleteAllOrganizations();
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected Class getPageClass()
    {
        return GithubConfigureOrganizationsPage.class;
    }

    @Test
    public void addOrganization()
    {
        configureOrganizations.addOrganizationSuccessfully(TEST_URL, TEST_ORGANIZATION, false);
        assertThat(configureOrganizations.getOrganizations().size(), equalTo(1));
    }

    @Test
    public void shouldBeAbleToSeePrivateRepositoriesFromTeamAccount()
    {
        // we should see 'private-dvcs-connector-test' repo
        configureOrganizations.addOrganizationSuccessfully(TEST_URL, "atlassian", false);

        assertThat(configureOrganizations.containsRepositoryWithName("private-dvcs-connector-test"), is(true));
    }

    @Test
    public void addUrlThatDoesNotExist()
    {
        configureOrganizations.addOrganizationFailingStep1(TEST_NOT_EXISTING_URL);

        String errorMessage = configureOrganizations.getErrorStatusMessage();
        assertThat(errorMessage, containsString("Invalid user/team account."));
        configureOrganizations.clearForm();
    }

    @Test
    public void testPostCommitHookAdded() throws Exception
    {
        String baseUrl = jira.getProductInstance().getBaseUrl();

        // add repository
        configureOrganizations.addOrganizationSuccessfully(TEST_URL, TEST_ORGANIZATION, true);

        // check that it created postcommit hook
        String githubServiceConfigUrlPath = baseUrl + "/rest/bitbucket/1.0/repository/";
        String hooksURL = "https://github.com/jirabitbucketconnector/test-project/admin/hooks";
        String hooksPage = getGithubServices(hooksURL, REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);
        assertThat(hooksPage, containsString(githubServiceConfigUrlPath));
        goToConfigPage();
        // delete repository
        configureOrganizations.deleteAllOrganizations();
        // check that postcommit hook is removed
        hooksPage = getGithubServices(hooksURL, REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);
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
    public void addRepoCommitsAppearOnIssues()
    {
        configureOrganizations.addOrganizationSuccessfully(TEST_URL, TEST_ORGANIZATION, true);

        assertThat(getCommitsForIssue("QA-2"),
                Matchers.<BitBucketCommitEntry>hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
        assertThat(getCommitsForIssue("QA-3"),
                Matchers.<BitBucketCommitEntry>hasItem(withMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA")));
    }

    @Test
    public void testCommitStatistics()
    {
        configureOrganizations.deleteAllOrganizations();
        configureOrganizations.addOrganizationSuccessfully(TEST_URL, TEST_ORGANIZATION, true);

        // QA-2
        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-3");
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


    @Test
    public void addPrivateRepoWithInvalidOAuth()
    {
        goToGithubOAuthConfigPage().setCredentials("xxx", "yyy");

        goToConfigPage();

        configureOrganizations.addRepoToProjectFailingStep2(TEST_URL);

        goToGithubOAuthConfigPage().setCredentials(clientID, clientSecret);
    }

    @Test
    public void addPrivateRepositoryWithValidOAuth()
    {
        GithubConfigureOrganizationsPage githubConfigPage = (GithubConfigureOrganizationsPage) goToConfigPage();

        GithubConfigureOrganizationsPage githubConfigureOrganizationsPage = githubConfigPage
                .addRepoToProjectForOrganization("dusanhornik");

        assertThat(githubConfigureOrganizationsPage.getNumberOfVisibleRepositories(), is(3));
    }

    private static Set<String> extractGithubHookIdsForRepositoryToRemove(String repositoryId)
    {
        String listHooksResponseString;
        String finalGithubUrl = String.format("https://api.github.com/repos/jirabitbucketconnector/%s/hooks",
                                              repositoryId);

        try
        {
            listHooksResponseString = HttpSenderUtils.sendGetHttpRequest(finalGithubUrl,
                                                                         REPO_ADMIN_LOGIN,
                                                                         REPO_ADMIN_PASSWORD);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot extract BitBucket service IDs !", e);
        }

        Set<String> extractedHookIds = new LinkedHashSet<String>();

        // parsing following JSON:
        //[
        //  {
        //    "url": "https://api.github.com/repos/octocat/Hello-World/hooks/1",
        //    "updated_at": "2011-09-06T20:39:23Z",
        //    "created_at": "2011-09-06T17:26:27Z",
        //    "name": "web",
        //    "events": [
        //      "push"
        //    ],
        //    "active": true,
        //    "config": {
        //      "url": "http://example.com",
        //      "content_type": "json"
        //    },
        //    "id": 1
        //  }
        //]

        try {
            JSONArray jsonArray = new JSONArray(listHooksResponseString);

            for (int i = 0; i < jsonArray.length(); i++)
            {
                JSONObject data = (JSONObject) jsonArray.get(i);

                String githubHookId = data.getString("id");
                JSONObject configObject = data.getJSONObject("config");
                String configURL = configObject.getString("url");

                if (configURL.contains(jira.getProductInstance().getBaseUrl())) {
                    extractedHookIds.add(githubHookId);
                }
            }
        }
        catch (JSONException e)
        {
            throw new IllegalStateException("Cannot parse JSON !", e);
        }

        return extractedHookIds;
    }

    private static void removePostCommitHook(String repositoryId, String serviceId) {
        String finalGithubUrl = String.format(
                "https://api.github.com/repos/jirabitbucketconnector/%s/hooks/%s",
                repositoryId,
                serviceId);
        try
        {
            HttpSenderUtils.sendDeleteHttpRequest(finalGithubUrl, REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Cannot send DELETE Http request !", e);
        }
    }
}
