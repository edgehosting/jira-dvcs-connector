package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccountRepository;
import com.atlassian.jira.plugins.dvcs.util.HttpSenderUtils;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.elements.PageElement;
import it.com.atlassian.jira.plugins.dvcs.DvcsWebDriverTestCase;
import it.restart.com.atlassian.jira.plugins.dvcs.DashboardActivityStreamsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.GreenHopperBoardPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraAddUserPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraMove_QA1_IssuePage;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController.AccountType;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoryDiv;
import org.apache.commons.httpclient.methods.GetMethod;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.List;

import static com.atlassian.jira.permission.ProjectPermissions.BROWSE_PROJECTS;
import static com.atlassian.jira.plugins.dvcs.pageobjects.BitBucketCommitEntriesAssert.assertThat;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static org.fest.assertions.api.Assertions.assertThat;

public class BitbucketTests extends DvcsWebDriverTestCase implements BasicTests, ActivityStreamsTest
{
    private static final String BB_ACCOUNT_NAME = "jirabitbucketconnector";
    private static final String OTHER_ACCOUNT_NAME = "dvcsconnectortest";
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private OAuth oAuth;
    private static final List<String> BASE_REPOSITORY_NAMES = Arrays.asList("public-hg-repo", "private-hg-repo", "public-git-repo", "private-git-repo");
    private static final String GADGET_ID = "gadget-10001";

    @BeforeClass
    public void beforeClass()
    {
        // log in to JIRA
        new JiraLoginPageController(jira).login();
        // log in to Bitbucket
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogin(BB_ACCOUNT_NAME, PasswordUtil.getPassword(BB_ACCOUNT_NAME));
        // setup up OAuth from bitbucket
        oAuth = new MagicVisitor(jira).visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).addConsumer();
        // jira.visit(JiraBitbucketOAuthPage.class).setCredentials(oAuth.key, oAuth.secret);
        jira.backdoor().plugins().disablePlugin("com.atlassian.jira.plugins.jira-development-integration-plugin");
    }

    @AfterClass
    public void afterClass()
    {
        // delete all organizations
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();
        // remove OAuth in bitbucket
        new MagicVisitor(jira).visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).removeConsumer(oAuth.applicationId);
        // log out from bitbucket
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogout();
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
        OrganizationDiv organization = addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositoryNames()).containsAll(BASE_REPOSITORY_NAMES);

        // check add user extension
        PageElement dvcsExtensionsPanel = jira.visit(JiraAddUserPage.class).getDvcsExtensionsPanel();
        assertThat(dvcsExtensionsPanel.isVisible());
    }

    @Override
    @Test
    public void addOrganizationWaitForSync()
    {
        OrganizationDiv organization = addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        assertThat(organization).isNotNull();
        assertThat(organization.getRepositoryNames()).containsAll(BASE_REPOSITORY_NAMES);
        final String repositoryName = "public-hg-repo";
        final String expectedMessage = "Fri Mar 02 2012";

        RepositoryDiv repositoryDiv = organization.findRepository(repositoryName);
        assertThat(repositoryDiv).isNotNull();
        assertThat(repositoryDiv.getMessage()).isEqualTo(expectedMessage);

        assertThat(getCommitsForIssue("QA-2", 1)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
        assertThat(getCommitsForIssue("QA-3", 2)).hasItemWithCommitMessage("BB modified 1 file to QA-2 and QA-3 from TestRepo-QA");
    }

    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe url \\[https://privatebitbucket.org\\] is incorrect or the server is not responding.*")
    public void addOrganizationInvalidUrl()
    {
        addOrganization(AccountType.BITBUCKET, "https://privatebitbucket.org/someaccount", getOAuthCredentials(), false, true);
    }

    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nInvalid user/team account.*")
    public void addOrganizationInvalidAccount()
    {
        addOrganization(AccountType.BITBUCKET, "I_AM_SURE_THIS_ACCOUNT_IS_INVALID", getOAuthCredentials(), false, true);
    }

    @Override
    @Test (expectedExceptions = AssertionError.class, expectedExceptionsMessageRegExp = ".*Error!\\nThe authentication with Bitbucket has failed. Please check your OAuth settings.*")
    public void addOrganizationInvalidOAuth()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, new OAuthCredentials("bad", "credentials"), true, true);
    }

    @Test
    @Override
    public void testCommitStatistics()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        // QA-2
        List<BitBucketCommitEntry> commitMessages = getCommitsForIssue("QA-2", 1); // throws AssertionError with other than 1 message

        BitBucketCommitEntry commitMessage = commitMessages.get(0);
        List<PageElement> statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();

        // QA-3
        commitMessages = getCommitsForIssue("QA-3", 2); // throws AssertionError with other than 2 messages

        // commit 1
        commitMessage = commitMessages.get(0);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.isAdded(statistics.get(0))).isTrue();
        // commit 2
        commitMessage = commitMessages.get(1);
        statistics = commitMessage.getStatistics();
        assertThat(statistics).hasSize(1);
        assertThat(commitMessage.getAdditions(statistics.get(0))).isEqualTo("+3");
        assertThat(commitMessage.getDeletions(statistics.get(0))).isEqualTo("-");
    }

    @Test
    @Override
    public void testPostCommitHookAddedAndRemoved()
    {
        testPostCommitHookAddedAndRemoved(AccountType.BITBUCKET, "public-hg-repo", jira, getOAuthCredentials());
    }

    @Override
    protected boolean postCommitHookExists(final String jiraCallbackUrl)
    {
        String bitbucketServiceConfigUrl = "https://bitbucket.org/!api/1.0/repositories/jirabitbucketconnector/public-hg-repo/services";

        String postDeleteServicesConfig = HttpSenderUtils.makeHttpRequest(new GetMethod(bitbucketServiceConfigUrl),
                BB_ACCOUNT_NAME, PasswordUtil.getPassword(BB_ACCOUNT_NAME));
        return postDeleteServicesConfig.contains(jiraCallbackUrl);
    }

    @Test
    @Override
    public void testActivityPresentedForQA5()
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        DashboardActivityStreamsPage page = visitActivityStreamGadget(GADGET_ID);

        // Activity streams should contain at least one changeset with 'more files' link.
        assertThat(page.isMoreFilesLinkVisible()).isTrue();
        page.checkIssueActivityPresentedForQA5();

        page.setIssueKeyFilter("QA-4");
        // because commit contains both keys QA-4 and QA-5, so should be present on both issues' activity streams
        page.checkIssueActivityPresentedForQA5();

        page.setIssueKeyFilter("QA-5");
        page.checkIssueActivityPresentedForQA5();

        // delete repository
        rpc = new RepositoriesPageController(jira);
        rpc.getPage().deleteAllOrganizations();

        page = visitActivityStreamGadget(GADGET_ID);
        page.checkIssueActivityNotPresentedForQA5();
    }

    @Override
    @Test
    public void testAnonymousAccess()
    {
        setupAnonymousAccessAllowed();
        try
        {
            // add organization
            addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

            // Activity streams gadget expected at dashboard page!
            DashboardActivityStreamsPage page = visitActivityStreamGadget(GADGET_ID);

            page.checkIssueActivityPresentedForQA5();

            // logout user
            jira.getTester().getDriver().manage().deleteAllCookies();

            page = visitActivityStreamGadget(GADGET_ID);
            // anonymous user should not see QA-5 activity stream
            page.checkIssueActivityNotPresentedForQA5();
        }
        finally
        {
            // always clean up the anonymous setting change
            new JiraLoginPageController(jira).login();
            setupAnonymousAccessForbidden();
        }
    }

    @Test
    public void greenHopperIntegration_ShouldAddDvcsCommitsTab()
    {
        // add organization
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        GreenHopperBoardPage greenHopperBoardPage = jira.getPageBinder().navigateToAndBind(GreenHopperBoardPage.class);
        greenHopperBoardPage.goToQABoardPlan();
        greenHopperBoardPage.assertCommitsAppearOnIssue("QA-1", 5);
    }

    @Test
    public void moveIssue_ShouldKeepAlsoCommits()
    {
        // add organization
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        // check commits setup to start with
        assertThat(getCommitsForIssue("QA-1", 5)).hasItemWithCommitMessage("QA-1 test modification");

        // move issue from QA project to BBC project
        JiraMove_QA1_IssuePage movingPage = jira.getPageBinder().navigateToAndBind(JiraMove_QA1_IssuePage.class, jira.getPageBinder());
        movingPage.stepOne_typeProjectName("Bitbucket Connector")
                .clickNext()
                .clickNext()
                .submit();

        // check commits kept
        // in fact, Jira will make the redirect to moved/created issue BBC-1
        assertThat(getCommitsForIssue("QA-1", 5)).hasItemWithCommitMessage("QA-1 test modification");
    }

    @Override
    @Test
    public void linkingRepositoryWithoutAdminPermission()
    {
        addOrganization(AccountType.BITBUCKET, OTHER_ACCOUNT_NAME, getOAuthCredentials(), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, OTHER_ACCOUNT_NAME);
        AccountsPageAccountRepository repository = account.enableRepository("testemptyrepo", true);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertTrue(repository.hasWarning());
    }

    @Override
    @Test
    public void linkingRepositoryWithAdminPermission()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, ACCOUNT_NAME);
        AccountsPageAccountRepository repository = account.enableRepository("private-git-repo", false);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertFalse(repository.hasWarning());
    }

    @Override
    @Test
    public void autoLinkingRepositoryWithoutAdminPermission()
    {
        addOrganization(AccountType.BITBUCKET, OTHER_ACCOUNT_NAME, getOAuthCredentials(), true);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, OTHER_ACCOUNT_NAME);

        for (AccountsPageAccountRepository repository : account.getRepositories())
        {
            Assert.assertTrue(repository.isEnabled());
        }
    }

    @Override
    @Test
    public void autoLinkingRepositoryWithAdminPermission()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), true);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, ACCOUNT_NAME);
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

    private void setupAnonymousAccessAllowed()
    {
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + "/secure/admin/AddPermission!default.jspa?schemeId=0&permissions=" + BROWSE_PROJECTS.permissionKey());
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("type_group"));
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("add_submit"));
        jira.getTester().getDriver().findElement(By.id("type_group")).click();
        jira.getTester().getDriver().findElement(By.id("add_submit")).click();
    }

    private void setupAnonymousAccessForbidden()
    {
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + "/secure/admin/EditPermissions!default.jspa?schemeId=0");
        String deleteLinkId = "del_perm_" + BROWSE_PROJECTS.permissionKey() + "_";
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id(deleteLinkId));
        jira.getTester().getDriver().findElement(By.id(deleteLinkId)).click();
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("delete_submit"));
        jira.getTester().getDriver().findElement(By.id("delete_submit")).click();
    }

    private OrganizationDiv addOrganization(AccountType accountType, String accountName, OAuthCredentials oAuthCredentials, boolean autosync)
    {

        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        try
        {
            return rpc.addOrganization(accountType, accountName, oAuthCredentials, autosync);
        }
        catch (NoSuchElementException e)
        {
            rpc = new RepositoriesPageController(jira);
            return rpc.addOrganization(accountType, accountName, oAuthCredentials, autosync);
        }
    }


    public OrganizationDiv addOrganization(AccountType accountType, String accountName, OAuthCredentials oAuthCredentials, boolean autosync, boolean expectError)
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        return rpc.addOrganization(accountType, accountName, oAuthCredentials, autosync, expectError);
    }

    private DashboardActivityStreamsPage visitActivityStreamGadget(final String gadgetId)
    {
        // Activity streams gadget expected at dashboard page!
        DashboardActivityStreamsPage page = jira.visit(DashboardActivityStreamsPage.class);
        assertThat(page.isActivityStreamsGadgetVisible()).isTrue();

        WebElement iframeElm = jira.getTester().getDriver().getDriver().findElement(By.id(gadgetId));
        String iframeSrc = iframeElm.getAttribute("src");
        jira.getTester().gotoUrl(iframeSrc);

        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);
        return page;
    }

    @Override
    public void shouldBeAbleToSeePrivateRepositoriesFromTeamAccount()
    {
        // Not relevant for Bitbucket - it uses same API for organizations and users
        // but maybe we will add something here one day
    }

}
