package it.com.atlassian.jira.plugins.dvcs;

import static com.atlassian.jira.plugins.dvcs.pageobjects.BitBucketCommitEntriesAssert.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.egit.github.core.RepositoryHook;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubRegisterOAuthAppPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.GithubRegisteredOAuthAppsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.elements.PageElement;

/**
 * Test to verify behaviour when syncing  github repository.
 */
public class GithubOrganizationsTest extends BaseOrganizationTest<GithubConfigureOrganizationsPage>
{

    private static final String TEST_ORGANIZATION = "jirabitbucketconnector";
    private static final String TEST_NOT_EXISTING_URL = "mynotexistingaccount124";
    private static final String REPO_ADMIN_LOGIN = "jirabitbucketconnector";
    private static final String REPO_ADMIN_PASSWORD = PasswordUtil.getPassword("jirabitbucketconnector");
    private static final String USER_AGENT = "DVCS Connector Test/X.x";

    private String oauthAppLink;
    private OAuthCredentials oAuthCredentials;

    @BeforeClass
    public void registerAppToGithub()
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
        oAuthCredentials = new OAuthCredentials(registerAppPage.getClientId().getText(), registerAppPage.getClientSecret().getText());

        // find out app URL
        jira.getTester().gotoUrl(GithubRegisteredOAuthAppsPage.PAGE_URL);
        GithubRegisteredOAuthAppsPage registeredOAuthAppsPage = jira.getPageBinder().bind(GithubRegisteredOAuthAppsPage.class);
        registeredOAuthAppsPage.parseClientIdAndSecret(oauthAppName);
        oauthAppLink = registeredOAuthAppsPage.getOauthAppUrl();
        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogout();

        // logout jira
        jira.getTester().getDriver().manage().deleteAllCookies();
    }

    @AfterClass
    public void deregisterAppToGithub()
    {
       /* jira.getTester().gotoUrl(GithubLoginPage.LOGOUT_ACTION_URL);

        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        GithubLoginPage ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogin();
*/

        jira.getTester().gotoUrl(oauthAppLink);

        GithubRegisterOAuthAppPage registerAppPage = jira.getPageBinder().bind(GithubRegisterOAuthAppPage.class);
        registerAppPage.deleteOAuthApp();

        jira.getTester().gotoUrl(GithubLoginPage.PAGE_URL);
        GithubLoginPage ghLoginPage = jira.getPageBinder().bind(GithubLoginPage.class);
        ghLoginPage.doLogout();
    }

    @BeforeMethod
    public void removeExistingPostCommitHooks() throws IOException
    {
        String[] githubRepositories = { "repo1", "test-project" };
        for (String githubRepositoryName : githubRepositories)
        {
            Set<Long> extractedGithubHookIds = extractGithubHookIdsForRepositoryToRemove(githubRepositoryName);
            for (long extractedGithubHookId : extractedGithubHookIds)
            {
                removePostCommitHook(githubRepositoryName, extractedGithubHookId);
            }
        }
    }

    @AfterMethod
    public void deleteRepositoriesAfterTest()
    {
        goToConfigPage();
        configureOrganizations.deleteAllOrganizations();
    }

    @Override
    protected Class<GithubConfigureOrganizationsPage> getConfigureOrganizationsPageClass()
    {
        return GithubConfigureOrganizationsPage.class;
    }

    @Test
    public void shouldBeAbleToSeePrivateRepositoriesFromTeamAccount()
    {
        // we should see 'private-dvcs-connector-test' repo
        configureOrganizations.addOrganizationSuccessfully("atlassian", oAuthCredentials, false);

        assertThat(configureOrganizations.containsRepositoryWithName("private-dvcs-connector-test")).isTrue();
    }

    @Test
    public void addUrlThatDoesNotExist()
    {
        configureOrganizations.addOrganizationFailingStep1(TEST_NOT_EXISTING_URL);

        String errorMessage = configureOrganizations.getErrorStatusMessage();
        assertThat(errorMessage).contains("Invalid user/team account.");
        configureOrganizations.clearForm();
    }

    @Test
    public void testPostCommitHookAdded()
    {
        String baseUrl = jira.getProductInstance().getBaseUrl();

        // add repository
        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, oAuthCredentials, true);

        // check that it created postcommit hook
        String githubServiceConfigUrlPath = baseUrl + "/rest/bitbucket/1.0/repository/";
        String hooksURL = "https://github.com/jirabitbucketconnector/test-project/settings/hooks";
        jira.getTester().gotoUrl(hooksURL);
        String hooksPage = jira.getTester().getDriver().getPageSource();
        assertThat(hooksPage).contains(githubServiceConfigUrlPath);
        goToConfigPage();
        // delete repository
        configureOrganizations.deleteAllOrganizations();
        // check that postcommit hook is removed
        jira.getTester().gotoUrl(hooksURL);
        hooksPage = jira.getTester().getDriver().getPageSource();
        assertThat(hooksPage).doesNotContain(githubServiceConfigUrlPath);
    }

    @Test
    public void addRepoCommitsAppearOnIssues()
    {
        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, oAuthCredentials, true);

        assertThat(getCommitsForIssue("QA-2", 6)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        assertThat(getCommitsForIssue("QA-3", 1)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
    }

    @Test
    public void testCommitStatistics()
    {
        configureOrganizations.deleteAllOrganizations();
        configureOrganizations.addOrganizationSuccessfully(TEST_ORGANIZATION, oAuthCredentials, true);

        // QA-2
        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-3", 1); // throws AssertionError with other than 1 message

        BitBucketCommitEntry commitMessage = commitMessages.get(0);
        List<PageElement> statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.getAdditions(statistics.get(0))).isEqualTo("+1");
        assertThat(commitMessage.getDeletions(statistics.get(0))).isEqualTo("-");

        // QA-4
        commitMessages = getCommitsForIssue("QA-4", 1); // throws AssertionError with other than 1 message

        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();
    }


    @Test
    public void addPrivateRepoWithInvalidOAuth()
    {
        goToConfigPage();
        configureOrganizations.addOrganizationFailingOAuth();
    }

    @Test
    public void addPrivateRepositoryWithValidOAuth()
    {
        GithubConfigureOrganizationsPage githubConfigPage = (GithubConfigureOrganizationsPage) goToConfigPage();
        GithubConfigureOrganizationsPage githubConfigureOrganizationsPage = githubConfigPage.addOrganizationSuccessfully("dusanhornik", oAuthCredentials, true);
        assertThat(githubConfigureOrganizationsPage.getNumberOfVisibleRepositories()).isEqualTo(4);
    }

    private static Set<Long> extractGithubHookIdsForRepositoryToRemove(String repositoryName) throws IOException
    {
        GitHubClient gitHubClient = GithubClientProvider.createClient("https://api.github.com", USER_AGENT);
        gitHubClient.setCredentials(REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);

        RepositoryService repositoryService = new RepositoryService(gitHubClient);

        RepositoryId repositoryId = RepositoryId.create(REPO_ADMIN_LOGIN, repositoryName);

        Set<Long> extractedHookIds = new LinkedHashSet<Long>();
        for (RepositoryHook repositoryHook : repositoryService.getHooks(repositoryId))
        {
            long githubHookId = repositoryHook.getId();
            String configURL = repositoryHook.getConfig().get("url");

            if (configURL.contains(jira.getProductInstance().getBaseUrl()))
            {
                extractedHookIds.add(githubHookId);
            }
        }

        return extractedHookIds;
    }

    private static void removePostCommitHook(String repositoryName, long hookId) throws IOException
    {
        GitHubClient gitHubClient = GithubClientProvider.createClient("https://api.github.com", USER_AGENT);
        gitHubClient.setCredentials(REPO_ADMIN_LOGIN, REPO_ADMIN_PASSWORD);

        RepositoryId repositoryId = RepositoryId.create(REPO_ADMIN_LOGIN, repositoryName);

        RepositoryService repositoryService = new RepositoryService(gitHubClient);
        repositoryService.deleteHook(repositoryId, (int) hookId);
    }
}
