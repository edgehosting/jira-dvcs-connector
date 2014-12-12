package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.pageobjects.model.DefaultIssueActions;
import com.atlassian.jira.pageobjects.pages.viewissue.IssueMenu;
import com.atlassian.jira.pageobjects.pages.viewissue.MoveIssuePage;
import com.atlassian.jira.pageobjects.pages.viewissue.ViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.OrganizationDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.RepositoryDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.JiraViewIssuePage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController.AccountType;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccountRepository;
import com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.ChangesetLocalRestpoint;
import com.atlassian.jira.plugins.dvcs.util.HttpSenderUtils;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.testing.rule.WebDriverSupport;
import it.com.atlassian.jira.plugins.dvcs.DvcsWebDriverTestCase;
import it.restart.com.atlassian.jira.plugins.dvcs.DashboardActivityStreamsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.GreenHopperBoardPage;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraAddUserPage;
import org.apache.commons.httpclient.methods.GetMethod;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.Point;
import org.openqa.selenium.WebDriver;
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
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        enableRepositoryAsAdmin("public-hg-repo").synchronize();

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
        rpc.addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        enableRepositoryAsAdmin("public-hg-repo").synchronize();

        DashboardActivityStreamsPage page = visitActivityStreamGadget(GADGET_ID, true);

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

        page = visitActivityStreamGadget(GADGET_ID, true);
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
            addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

            enableRepositoryAsAdmin("public-hg-repo").synchronize();

            // Activity streams gadget expected at dashboard page!
            DashboardActivityStreamsPage page = visitActivityStreamGadget(GADGET_ID, false);

            page.checkIssueActivityPresentedForQA5();

            // logout user
            jira.getTester().getDriver().manage().deleteAllCookies();

            page = visitActivityStreamGadget(GADGET_ID, false);
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
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        enableRepositoryAsAdmin("public-hg-repo").synchronize();

        setSize(new Dimension(1024, 1280));

        GreenHopperBoardPage greenHopperBoardPage = jira.getPageBinder().navigateToAndBind(GreenHopperBoardPage.class);
        greenHopperBoardPage.goToQABoardPlan();
        greenHopperBoardPage.assertCommitsAppearOnIssue("QA-1", 5);
    }

    // code copied from WindowSizeRule from atlassian-selenium,
    // will be reworked to have a proper TestNG implementation of the WindowSize annotation
    private void setSize(Dimension dimension)
    {
        final WebDriverSupport<? extends WebDriver> support = WebDriverSupport.fromAutoInstall();

        support.getDriver().manage().window().setPosition(new Point(0, 0));
        support.getDriver().manage().window().setSize(dimension);
        // _not_ a mistake... don't ask
        support.getDriver().manage().window().setSize(dimension);
    }

    @Test
    public void moveIssue_ShouldKeepAlsoCommits()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        enableRepositoryAsAdmin("public-hg-repo").synchronize();

        final String issueKey = "QA-1";

        final String commitMessage = "QA-1 test modification";
        final int numberOfCommits = 5;

        ChangesetLocalRestpoint changesetLocalRestpoint = new ChangesetLocalRestpoint();
        List<String> originalCommitMessages = changesetLocalRestpoint.retryingGetCommitMessages(issueKey, numberOfCommits);
        assertThat(originalCommitMessages).contains(commitMessage);
//        checkIssueKeyHasCommitWithMessage(issueKey, numberOfCommits, commitMessage);

        jira.gotoHomePage();
        // move issue from QA project to BBC project
        moveIssueToProject(issueKey, "Bitbucket Connector");

//        checkIssueKeyHasCommitWithMessage(issueKey, numberOfCommits, commitMessage);
        List<String> movedCommitMessages = changesetLocalRestpoint.retryingGetCommitMessages(issueKey, numberOfCommits);
        assertThat(movedCommitMessages).contains(commitMessage);
    }

    @Override
    @Test
    public void linkingRepositoryWithoutAdminPermission()
    {
        addOrganization(AccountType.BITBUCKET, OTHER_ACCOUNT_NAME, getOAuthCredentials(), false);

        AccountsPageAccountRepository repository = enableRepository(OTHER_ACCOUNT_NAME, "testemptyrepo", true);

        // check that repository is enabled
        Assert.assertTrue(repository.isEnabled());
        Assert.assertTrue(repository.hasWarning());
    }

    @Override
    @Test
    public void linkingRepositoryWithAdminPermission()
    {
        addOrganization(AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);

        AccountsPageAccountRepository repository = enableRepositoryAsAdmin("private-git-repo");

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
                .waitForNumberOfMessages(exectedNumberOfCommits, 1000L, 10);
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

    private DashboardActivityStreamsPage visitActivityStreamGadget(final String gadgetId, final boolean isEditMode)
    {
        // Activity streams gadget expected at dashboard page!
        DashboardActivityStreamsPage page = jira.visit(DashboardActivityStreamsPage.class, isEditMode);
        assertThat(page.isActivityStreamsGadgetVisible()).isTrue();

        WebElement iframeElm = jira.getTester().getDriver().getDriver().findElement(By.id(gadgetId));
        String iframeSrc = iframeElm.getAttribute("src");
        jira.getTester().gotoUrl(iframeSrc);

        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class, isEditMode);
        return page;
    }

    /**
     * enable a repo under the {@link IntegrationTestUserDetails#ACCOUNT_NAME} account.
     */
    private AccountsPageAccountRepository enableRepositoryAsAdmin(final String repositoryName)
    {
        return enableRepositoryAsAdmin(ACCOUNT_NAME, repositoryName);
    }

    private AccountsPageAccountRepository enableRepositoryAsAdmin(final String accountName, final String repositoryName)
    {
        return enableRepository(accountName, repositoryName, false);
    }

    private AccountsPageAccountRepository enableRepository(final String accountName, final String repositoryName, final boolean noAdminPermission)
    {
        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, accountName);
        return account.enableRepository(repositoryName, noAdminPermission);
    }

    private void moveIssueToProject(final String issueKey, final String newProject)
    {
        ViewIssuePage viewIssuePage = jira.goToViewIssue(issueKey);
        IssueMenu issueMenu = viewIssuePage.getIssueMenu();
        issueMenu.invoke(DefaultIssueActions.MOVE);
        final MoveIssuePage moveIssuePage = jira.getPageBinder().bind(MoveIssuePage.class, issueKey);
        moveIssuePage.setNewProject(newProject).next().next().move();
    }

    @Override
    public void shouldBeAbleToSeePrivateRepositoriesFromTeamAccount()
    {
        // Not relevant for Bitbucket - it uses same API for organizations and users
        // but maybe we will add something here one day
    }

}
