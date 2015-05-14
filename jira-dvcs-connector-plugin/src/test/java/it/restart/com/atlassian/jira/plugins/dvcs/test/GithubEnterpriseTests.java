package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.OrganizationDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccountRepository;
import com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.ChangesetLocalRestpoint;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.elements.PageElement;
import it.com.atlassian.jira.plugins.dvcs.DvcsWebDriverTestCase;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthApplicationPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthPage;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static com.atlassian.jira.plugins.dvcs.pageobjects.BitBucketCommitEntriesAssert.assertThat;
import static com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController.AccountType.getGHEAccountType;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.GithubTestHelper.REPOSITORY_NAME;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static it.util.TestAccounts.JIRA_BB_CONNECTOR_ACCOUNT;
import static it.util.TestAccounts.DVCS_CONNECTOR_TEST_ACCOUNT;
import static org.fest.assertions.api.Assertions.assertThat;

public class GithubEnterpriseTests extends DvcsWebDriverTestCase implements BasicTests
{
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    public static final String GITHUB_ENTERPRISE_URL = System.getProperty("githubenterprise.url", "http://192.168.2.214");
    private OAuth oAuth;

    private static final List<String> BASE_REPOSITORY_NAMES = Arrays.asList(new String[] { "missingcommits", "repo1", "test", "test-project", "noauthor" });

    @BeforeClass
    public void beforeClass()
    {
        jira.backdoor().restoreDataFromResource(TEST_DATA);
        // log in to JIRA 
        new JiraLoginPageController(jira).login();
        // log in to github enterprise
        new MagicVisitor(jira).visit(GithubLoginPage.class, GITHUB_ENTERPRISE_URL).doLogin();

        // setup up OAuth from github
        oAuth = new MagicVisitor(jira).visit(GithubOAuthPage.class, GITHUB_ENTERPRISE_URL)
                .addConsumer(jira.getProductInstance().getBaseUrl());
        jira.backdoor().plugins().disablePlugin("com.atlassian.jira.plugins.jira-development-integration-plugin");
    }

    @AfterClass
    public void afterClass()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
        // remove OAuth in github enterprise
        new MagicVisitor(jira).visit(GithubOAuthApplicationPage.class, GITHUB_ENTERPRISE_URL).removeConsumer(oAuth);
        // log out from github enterprise
        new MagicVisitor(jira).visit(GithubLoginPage.class, GITHUB_ENTERPRISE_URL).doLogout();
    }

    @BeforeMethod
    public void beforeMethod()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
    }

    @Override
    @Test
    public void shouldBeAbleToSeePrivateRepositoriesFromTeamAccount()
    {
        // we should see 'private-dvcs-connector-test' repo
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(getGHEAccountType(GITHUB_ENTERPRISE_URL), "atlassian", new OAuthCredentials(oAuth.key, oAuth.secret), false);

        assertThat(organization.containsRepository("private-dvcs-connector-test"));
    }

    @Test
    @Override
    public void addOrganization()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(getGHEAccountType(GITHUB_ENTERPRISE_URL), ACCOUNT_NAME,
                new OAuthCredentials(oAuth.key, oAuth.secret), false);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositoryNames()).containsAll(BASE_REPOSITORY_NAMES);
    }

    @Test
    @Override
    public void addOrganizationWaitForSync()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(getGHEAccountType(GITHUB_ENTERPRISE_URL), ACCOUNT_NAME,
                new OAuthCredentials(oAuth.key, oAuth.secret), true);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositoryNames()).containsAll(BASE_REPOSITORY_NAMES);

        ChangesetLocalRestpoint changesetLocalRestpoint = new ChangesetLocalRestpoint();
        List<String> commitsForQA2 = changesetLocalRestpoint.getCommitMessages("QA-2", 6);
        assertThat(commitsForQA2).contains("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        List<String> commitsForQA3 = changesetLocalRestpoint.getCommitMessages("QA-3", 1);
        assertThat(commitsForQA3).contains("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
    }


    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\n.*Error retrieving list of repositories.*")
    public void addOrganizationInvalidAccount()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(getGHEAccountType(GITHUB_ENTERPRISE_URL), "I_AM_SURE_THIS_ACCOUNT_IS_INVALID",
                getOAuthCredentials(), false, true);
    }

    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe url \\[https://nonexisting.org\\] is incorrect or the server is not responding.*")
    public void addOrganizationInvalidUrl()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(getGHEAccountType(GITHUB_ENTERPRISE_URL), "https://nonexisting.org/someaccount",
                getOAuthCredentials(), false, true);
    }

    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = "Invalid OAuth")
    public void addOrganizationInvalidOAuth()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(getGHEAccountType(GITHUB_ENTERPRISE_URL), ACCOUNT_NAME,
                new OAuthCredentials("xxx", "yyy"), true, true);
    }

    @Test
    @Override
    public void testCommitStatistics()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(getGHEAccountType(GITHUB_ENTERPRISE_URL), ACCOUNT_NAME,
                new OAuthCredentials(oAuth.key, oAuth.secret), true);

        // QA-2
        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-3", 1);
        assertThat(commitMessages).hasSize(1);

        BitBucketCommitEntry commitMessage = commitMessages.get(0);
        List<PageElement> statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.getAdditions(statistics.get(0))).isEqualTo("+1");
        assertThat(commitMessage.getDeletions(statistics.get(0))).isEqualTo("-");

        // QA-4
        commitMessages = getCommitsForIssue("QA-4", 1);
        assertThat(commitMessages).hasSize(1);

        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();
    }

    private OAuthCredentials getOAuthCredentials()
    {
        return new OAuthCredentials(oAuth.key, oAuth.secret);
    }

    @Override
    @Test
    public void testPostCommitHookAddedAndRemoved()
    {
        RepositoriesPageController.AccountType gheAccountType = getGHEAccountType(GITHUB_ENTERPRISE_URL);
        testPostCommitHookAddedAndRemoved(JIRA_BB_CONNECTOR_ACCOUNT, gheAccountType, REPOSITORY_NAME, jira, getOAuthCredentials());
    }

    @Override
    protected boolean postCommitHookExists(final String accountName, final String jiraCallbackUrl)
    {
        List<String> actualHookUrls = GithubTestHelper.getHookUrls(accountName, GITHUB_ENTERPRISE_URL, REPOSITORY_NAME);
        return actualHookUrls.contains(jiraCallbackUrl);
    }

    @Test
    public void linkingRepositoryWithoutAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        RepositoriesPageController.AccountType accountType = RepositoriesPageController.AccountType.getGHEAccountType(GITHUB_ENTERPRISE_URL);
        rpc.addOrganization(accountType, DVCS_CONNECTOR_TEST_ACCOUNT, getOAuthCredentials(), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB_ENTERPRISE, DVCS_CONNECTOR_TEST_ACCOUNT);
        AccountsPageAccountRepository repository = account.enableRepository("testemptyrepo", true);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertTrue(repository.hasWarning());
    }

    @Test
    public void linkingRepositoryWithAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        RepositoriesPageController.AccountType accountType = RepositoriesPageController.AccountType.getGHEAccountType(GITHUB_ENTERPRISE_URL);
        rpc.addOrganization(accountType, ACCOUNT_NAME, getOAuthCredentials(), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB_ENTERPRISE, ACCOUNT_NAME);
        AccountsPageAccountRepository repository = account.enableRepository(REPOSITORY_NAME, false);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertFalse(repository.hasWarning());
    }

    @Test
    public void autoLinkingRepositoryWithoutAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        RepositoriesPageController.AccountType accountType = RepositoriesPageController.AccountType.getGHEAccountType(GITHUB_ENTERPRISE_URL);
        rpc.addOrganization(accountType, DVCS_CONNECTOR_TEST_ACCOUNT, getOAuthCredentials(), true);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB_ENTERPRISE, DVCS_CONNECTOR_TEST_ACCOUNT);

        for (AccountsPageAccountRepository repository : account.getRepositories())
        {
            Assert.assertTrue(repository.isEnabled());
        }
    }

    @Test
    public void autoLinkingRepositoryWithAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        RepositoriesPageController.AccountType accountType = RepositoriesPageController.AccountType.getGHEAccountType(GITHUB_ENTERPRISE_URL);
        rpc.addOrganization(accountType, ACCOUNT_NAME, getOAuthCredentials(), true);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB_ENTERPRISE, ACCOUNT_NAME);

        for (AccountsPageAccountRepository repository : account.getRepositories())
        {
            Assert.assertTrue(repository.isEnabled());
            Assert.assertFalse(repository.hasWarning());
        }
    }

    private List<BitBucketCommitEntry> getCommitsForIssue(String issueKey, int exectedNumberOfCommits)
    {
        return jira.visit(JiraViewIssuePage.class, issueKey)
                .openBitBucketPanel()
                .waitForNumberOfMessages(exectedNumberOfCommits, 1000L, 5);
    }

}
