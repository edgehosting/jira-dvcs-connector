package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.OrganizationDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.RepositoryDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController.AccountType;
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

import static it.restart.com.atlassian.jira.plugins.dvcs.test.GithubTestHelper.GITHUB_API_URL;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.GithubTestHelper.REPOSITORY_NAME;
import static it.util.TestAccounts.DVCS_CONNECTOR_TEST_ACCOUNT;
import static it.util.TestAccounts.JIRA_BB_CONNECTOR_ACCOUNT;
import static org.fest.assertions.api.Assertions.assertThat;

public class GithubTests extends DvcsWebDriverTestCase implements BasicTests
{
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private static final List<String> BASE_REPOSITORY_NAMES = Arrays.asList(new String[] { "missingcommits", "repo1", "noauthor", "test-project" });
    private OAuth oAuth;

    @BeforeClass
    public void beforeClass()
    {
        jira.backdoor().restoreDataFromResource(TEST_DATA);
        // log in to JIRA
        new JiraLoginPageController(jira).login();
        // log in to github
        new MagicVisitor(jira).visit(GithubLoginPage.class).doLogin();
        // setup up OAuth from github
        oAuth = new MagicVisitor(jira).visit(GithubOAuthPage.class).addConsumer(jira.getProductInstance().getBaseUrl());
        jira.backdoor().plugins().disablePlugin("com.atlassian.jira.plugins.jira-development-integration-plugin");
    }

    @AfterClass
    public void afterClass()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
        // remove OAuth in github
        new MagicVisitor(jira).visit(GithubOAuthApplicationPage.class).removeConsumer(oAuth);

        // log out from github
        new MagicVisitor(jira).visit(GithubLoginPage.class).doLogout();
    }

    @BeforeMethod
    public void beforeMethod()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
    }

    @Override
    @Test
    public void addOrganization()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT,
                getOAuthCredentials(), false);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositoryNames()).containsAll(BASE_REPOSITORY_NAMES);
    }

    @Override
    @Test
    public void addOrganizationWaitForSync()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT,
                getOAuthCredentials(), false);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositoryNames()).containsAll(BASE_REPOSITORY_NAMES);

        final String expectedMessage = "Mon Feb 06 2012";
        final RepositoryDiv repositoryDiv = organization.findRepository(REPOSITORY_NAME);
        assertThat(repositoryDiv).isNotNull();
        repositoryDiv.enableSync();
        repositoryDiv.sync();
        assertThat(repositoryDiv.getMessage()).isEqualTo(expectedMessage);

        ChangesetLocalRestpoint changesetLocalRestpoint = new ChangesetLocalRestpoint();
        List<String> commitsForQA2 = changesetLocalRestpoint.getCommitMessages("QA-2", 6);
        assertThat(commitsForQA2).contains("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        List<String> commitsForQA3 = changesetLocalRestpoint.getCommitMessages("QA-3", 1);
        assertThat(commitsForQA3).contains("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
    }

    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe url \\[https://nonexisting.org\\] is incorrect or the server is not responding.*")
    public void addOrganizationInvalidUrl()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, "https://nonexisting.org/someaccount", getOAuthCredentials(), false, true);
    }

    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = "Invalid OAuth")
    public void addOrganizationInvalidOAuth()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT, new OAuthCredentials("xxx", "yyy"), true, true);
    }

    @Test
    @Override
    public void testPostCommitHookAddedAndRemoved()
    {
        testPostCommitHookAddedAndRemoved(JIRA_BB_CONNECTOR_ACCOUNT, AccountType.GITHUB, REPOSITORY_NAME, jira, getOAuthCredentials());
    }

    @Override
    protected boolean postCommitHookExists(final String accountName, final String jiraCallbackUrl)
    {
        List<String> actualHookUrls = GithubTestHelper.getHookUrls(accountName, GITHUB_API_URL, REPOSITORY_NAME);
        return actualHookUrls.contains(jiraCallbackUrl);
    }

    @Test
    @Override
    public void testCommitStatistics()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        final OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT,
                getOAuthCredentials(), false);
        final RepositoryDiv repositoryDiv = organization.findRepository("test-project");
        repositoryDiv.enableSync();
        repositoryDiv.sync();

        // QA-2
        List<BitBucketCommitEntry> commitMessages = new JiraViewIssuePageController(jira, "QA-3").getCommits(1); // throws AssertionError with other than 1 message
        assertThat(commitMessages).hasSize(1);

        BitBucketCommitEntry commitMessage = commitMessages.get(0);
        List<PageElement> statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.getAdditions(statistics.get(0))).isEqualTo("+1");
        assertThat(commitMessage.getDeletions(statistics.get(0))).isEqualTo("-");

        // QA-4
        commitMessages = new JiraViewIssuePageController(jira, "QA-4").getCommits(1); // throws AssertionError with other than 1 message
        assertThat(commitMessages).hasSize(1);

        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();
    }

    @Override
    @Test
    public void shouldBeAbleToSeePrivateRepositoriesFromTeamAccount()
    {
        // we should see 'private-dvcs-connector-test' repo
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, "atlassian",
                getOAuthCredentials(), false);

        assertThat(organization.containsRepository("private-dvcs-connector-test"));
    }

    @Test
    public void linkingRepositoryWithoutAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, DVCS_CONNECTOR_TEST_ACCOUNT, getOAuthCredentials(), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB, DVCS_CONNECTOR_TEST_ACCOUNT);
        AccountsPageAccountRepository repository = account.enableRepository("testemptyrepo", true);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertTrue(repository.hasWarning());
    }

    @Test
    public void linkingRepositoryWithAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT, getOAuthCredentials(), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB, JIRA_BB_CONNECTOR_ACCOUNT);
        AccountsPageAccountRepository repository = account.enableRepository(REPOSITORY_NAME, false);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertFalse(repository.hasWarning());
    }

    @Test
    public void autoLinkingRepositoryWithoutAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        final OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, DVCS_CONNECTOR_TEST_ACCOUNT, getOAuthCredentials(), false);
        organization.enableAllRepos();

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB, DVCS_CONNECTOR_TEST_ACCOUNT);

        for (AccountsPageAccountRepository repository : account.getRepositories())
        {
            Assert.assertTrue(repository.isEnabled());
        }
    }

    @Test
    public void autoLinkingRepositoryWithAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        final OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, JIRA_BB_CONNECTOR_ACCOUNT, getOAuthCredentials(), false);
        organization.enableAllRepos();

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB, JIRA_BB_CONNECTOR_ACCOUNT);

        for (AccountsPageAccountRepository repository : account.getRepositories())
        {
            Assert.assertTrue(repository.isEnabled());
            Assert.assertFalse(repository.hasWarning());
        }
    }

    //-------------------------------------------------------------------
    //--------- these methods should go to some common utility/class ----
    //-------------------------------------------------------------------

    private OAuthCredentials getOAuthCredentials()
    {
        return new OAuthCredentials(oAuth.key, oAuth.secret);
    }

    private List<BitBucketCommitEntry> getCommitsForIssue(String issueKey, int exectedNumberOfCommits)
    {
        return jira.visit(JiraViewIssuePage.class, issueKey)
                .openBitBucketPanel()
                .waitForNumberOfMessages(exectedNumberOfCommits, 1000L, 5);
    }

}
