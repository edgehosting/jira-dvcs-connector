package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.util.HttpSenderUtils;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import it.com.atlassian.jira.plugins.dvcs.DvcsWebDriverTestCase;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController.AccountType;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoryDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthApplicationPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static com.atlassian.jira.plugins.dvcs.pageobjects.BitBucketCommitEntriesAssert.assertThat;
import static org.fest.assertions.api.Assertions.assertThat;

public class GithubTests extends DvcsWebDriverTestCase implements BasicTests
{
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private static final String ACCOUNT_NAME = "jirabitbucketconnector";
    private static final String OTHER_ACCOUNT_NAME = "dvcsconnectortest";

    private OAuth oAuth;

    @BeforeClass
    public void beforeClass()
    {
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
        OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, ACCOUNT_NAME, getOAuthCredentials(), false);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositories(true).size()).isEqualTo(4);
    }

    @Override
    @Test
    public void addOrganizationWaitForSync()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organization = rpc.addOrganization(AccountType.GITHUB, ACCOUNT_NAME, getOAuthCredentials(),true);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositories(true).size()).isEqualTo(4);

        Poller.waitUntil(organization.getRepositories(true).get(3).getSyncIcon().timed().hasClass("running"), Matchers.is(false), Poller.by(2000));

        assertThat(organization.getRepositories(true).get(3).getMessage()).isEqualTo("Mon Feb 06 2012");

        assertThat(getCommitsForIssue("QA-2", 6)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        assertThat(getCommitsForIssue("QA-3", 1)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
    }

    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe url \\[https://nonexisting.org\\] is incorrect or the server is not responding.*")
    public void addOrganizationInvalidUrl()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, "https://nonexisting.org/someaccount", getOAuthCredentials(), false, true);
    }

    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nInvalid user/team account.*")
    public void addOrganizationInvalidAccount()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, "I_AM_SURE_THIS_ACCOUNT_IS_INVALID", getOAuthCredentials(), false, true);
    }


    @Override
    @Test(expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = "Invalid OAuth")
    public void addOrganizationInvalidOAuth()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, ACCOUNT_NAME, new OAuthCredentials("xxx", "yyy"), true, true);
    }

    @Test
    @Override
    public void testPostCommitHookAdded()
    {
        // remove existing hooks
        String hooksUrl = "https://api.github.com/repos/jirabitbucketconnector/test-project/hooks";
        HttpSenderUtils.removeJsonElementsUsingIDs(hooksUrl, "jirabitbucketconnector", PasswordUtil.getPassword("jirabitbucketconnector"));

        // add organization
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organisation = rpc.addOrganization(AccountType.GITHUB, ACCOUNT_NAME, getOAuthCredentials(), true);

        // check that it created postcommit hook
        String githubServiceConfigUrlPath = jira.getProductInstance().getBaseUrl() + "/rest/bitbucket/1.0/repository/";

        RepositoryDiv createdOrganisation = organisation.findRepository("test-project");
        if (createdOrganisation != null)
        {
            githubServiceConfigUrlPath += createdOrganisation.getRepositoryId() + "/sync";
        }

        String hooksURL = "https://github.com/jirabitbucketconnector/test-project/settings/hooks";
        jira.getTester().gotoUrl(hooksURL);
        String hooksPage = jira.getTester().getDriver().getPageSource();

        if (!hooksPage.contains(githubServiceConfigUrlPath))
        {
            // let's retry once more
            jira.getTester().gotoUrl(hooksURL);
            hooksPage = jira.getTester().getDriver().getPageSource();
        }

        assertThat(hooksPage).contains(githubServiceConfigUrlPath);
        // delete repository
        new RepositoriesPageController(jira).getPage().deleteAllOrganizations();
        // check that postcommit hook is removed
        jira.getTester().gotoUrl(hooksURL);
        hooksPage = jira.getTester().getDriver().getPageSource();
        assertThat(hooksPage).doesNotContain(githubServiceConfigUrlPath);
    }

    @Test
    @Override
    public void testCommitStatistics()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, ACCOUNT_NAME, getOAuthCredentials(), true);

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
        rpc.addOrganization(AccountType.GITHUB, OTHER_ACCOUNT_NAME, getOAuthCredentials(), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB, OTHER_ACCOUNT_NAME);
        AccountsPageAccountRepository repository = account.enableRepository("testemptyrepo", true);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertTrue(repository.hasWarning());
    }

    @Test
    public void linkingRepositoryWithAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, ACCOUNT_NAME, getOAuthCredentials(), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB, ACCOUNT_NAME);
        AccountsPageAccountRepository repository = account.enableRepository("test-project", false);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertFalse(repository.hasWarning());
    }

    @Test
    public void autoLinkingRepositoryWithoutAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, OTHER_ACCOUNT_NAME, getOAuthCredentials(), true);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB, OTHER_ACCOUNT_NAME);

        for (AccountsPageAccountRepository repository : account.getRepositories())
        {
            Assert.assertTrue(repository.isEnabled());
        }
    }

    @Test
    public void autoLinkingRepositoryWithAdminPermission()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.GITHUB, ACCOUNT_NAME, getOAuthCredentials(), true);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.GIT_HUB, ACCOUNT_NAME);

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
