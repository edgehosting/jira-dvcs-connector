package it.com.atlassian.jira.plugins.bitbucket.streams;

import com.atlassian.jira.plugins.bitbucket.pageobjects.page.BitBucketConfigureRepositoriesPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.DashboardActivityStreamsPage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.atlassian.webdriver.jira.page.DashboardPage;
import it.com.atlassian.jira.plugins.bitbucket.BitBucketBaseTest.AnotherLoginPage;
import junit.framework.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 *
 */
public class ActivityStreamsTest
{
    protected static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private DashboardActivityStreamsPage page;


    private void loginToJira()
    {
        jira.getPageBinder().override(LoginPage.class, AnotherLoginPage.class);
        jira.getPageBinder().navigateToAndBind(AnotherLoginPage.class).loginAsSysAdmin(DashboardPage.class);

    }

    private void addRepo()
    {

        BitBucketConfigureRepositoriesPage configureRepos = goToRepositoriesConfigPage();
        configureRepos.deleteAllRepositories();
        configureRepos.addPublicRepoToProjectSuccessfully("QA", "https://bitbucket.org/jirabitbucketconnector/public-hg-repo");
    }

    private void logout()
    {
        jira.getTester().getDriver().manage().deleteAllCookies();
    }


    private void setupAnonymousAccessAllowed()
    {
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + "/secure/admin/AddPermission!default.jspa?schemeId=0&permissions=10");
        jira.getTester().getDriver().findElement(By.id("type_group")).setSelected();
        jira.getTester().getDriver().findElement(By.id("add_submit")).click();
    }


    private void setupAnonymousAccessForbidden()
    {
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + "/secure/admin/EditPermissions!default.jspa?schemeId=0");
        jira.getTester().getDriver().findElement(By.id("del_perm_10_")).click();
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("delete_submit"));
        jira.getTester().getDriver().findElement(By.id("delete_submit")).click();
    }

    private BitBucketConfigureRepositoriesPage goToRepositoriesConfigPage()
    {
        BitBucketConfigureRepositoriesPage configureRepos = jira.visit(BitBucketConfigureRepositoriesPage.class);
        configureRepos.setJiraTestedProduct(jira);
        return configureRepos;
    }


    private void goToDashboardPage()
    {
        page = jira.visit(DashboardActivityStreamsPage.class);
        page.setJira(jira);
    }

    private void bindPageAndSetJira()
    {
        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);
        page.setJira(jira);
    }


    @Test
    public void testActivityPresentedForQA5()
    {
        loginToJira();
        addRepo();
        goToDashboardPage();

        Assert.assertTrue("Activity streams gadget expected at dashboard page!", page.isActivityStreamsGadgetVisible());

        WebElement iframeElm = jira.getTester().getDriver().getDriver().findElement(By.id("gadget-10001"));
        String iframeSrc = iframeElm.getAttribute("src");
        jira.getTester().gotoUrl(iframeSrc);
        bindPageAndSetJira();

        page.checkIssueActivityPresentedForQA5();

        page.setIssueKeyFilter("qa-4");
        bindPageAndSetJira();

        page.checkIssueActivityNotPresentedForQA5();

        page.setIssueKeyFilter("qa-5");
        bindPageAndSetJira();

        page.checkIssueActivityPresentedForQA5();

        goToRepositoriesConfigPage().deleteAllRepositories();

        goToDashboardPage();
        bindPageAndSetJira();

        page.checkIssueActivityNotPresentedForQA5();

        logout();
    }

    @Test
    public void testAnonymousAccess()
    {
        loginToJira();
        setupAnonymousAccessAllowed();
        addRepo();
        goToDashboardPage();

        Assert.assertTrue("Activity streams gadget expected at dashboard page!", page.isActivityStreamsGadgetVisible());

        WebElement iframeElm = jira.getTester().getDriver().getDriver().findElement(By.id("gadget-10001"));
        String iframeSrc = iframeElm.getAttribute("src");
        jira.getTester().gotoUrl(iframeSrc);
        bindPageAndSetJira();

        page.checkIssueActivityPresentedForQA5();

        logout();
        jira.getPageBinder().navigateToAndBind(DashboardPage.class);

        Assert.assertTrue("Activity streams gadget expected at dashboard page!", page.isActivityStreamsGadgetVisible());

        jira.getTester().gotoUrl(iframeSrc);
        bindPageAndSetJira();

        page.checkIssueActivityNotPresentedForQA5();

        loginToJira();
        setupAnonymousAccessForbidden();
        logout();
    }
}