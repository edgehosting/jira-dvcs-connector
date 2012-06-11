package it.com.atlassian.jira.plugins.bitbucket.streams;

import it.com.atlassian.jira.plugins.dvcs.BitBucketBaseOrgTest.AnotherLoginPage;
import junit.framework.Assert;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitBucketConfigureOrganizationsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.DashboardActivityStreamsPage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.atlassian.webdriver.jira.page.DashboardPage;

/**
 *
 */
public class ActivityStreamsTest
{
	private static final String BB_TEST_URL = "https://bitbucket.org";

    protected static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private DashboardActivityStreamsPage page;


    private void loginToJira()
    {
        jira.getPageBinder().override(LoginPage.class, AnotherLoginPage.class);
        jira.getPageBinder().navigateToAndBind(AnotherLoginPage.class).loginAsSysAdmin(DashboardPage.class);
    }

    private void addOrganization()
    {
    	BitBucketConfigureOrganizationsPage configureRepos = goToConfigPage();
        configureRepos.deleteAllOrganizations();
        configureRepos.addOrganizationSuccessfully(BB_TEST_URL, true);
    }


    private void logout()
    {
        jira.getTester().getDriver().manage().deleteAllCookies();
    }


    private void setupAnonymousAccessAllowed()
    {
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + "/secure/admin/AddPermission!default.jspa?schemeId=0&permissions=10");
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("type_group"));
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("add_submit"));
        jira.getTester().getDriver().findElement(By.id("type_group")).setSelected();
        jira.getTester().getDriver().findElement(By.id("add_submit")).click();
    }


    private void setupAnonymousAccessForbidden()
    {
        jira.getTester().gotoUrl(jira.getProductInstance().getBaseUrl() + "/secure/admin/EditPermissions!default.jspa?schemeId=0");
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("del_perm_10_"));
        jira.getTester().getDriver().findElement(By.id("del_perm_10_")).click();
        jira.getTester().getDriver().waitUntilElementIsVisible(By.id("delete_submit"));
        jira.getTester().getDriver().findElement(By.id("delete_submit")).click();
    }

    private BitBucketConfigureOrganizationsPage goToConfigPage()
    {
    	BitBucketConfigureOrganizationsPage configureRepos = jira.visit(BitBucketConfigureOrganizationsPage.class);
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
        addOrganization();
        goToDashboardPage();

        Assert.assertTrue("Activity streams gadget expected at dashboard page!", page.isActivityStreamsGadgetVisible());

        WebElement iframeElm = jira.getTester().getDriver().getDriver().findElement(By.id("gadget-10001"));
        String iframeSrc = iframeElm.getAttribute("src");
        jira.getTester().gotoUrl(iframeSrc);
        bindPageAndSetJira();

        Assert.assertTrue("Activity streams should contain at least one changeset with 'more files' link.", page.isMoreFilesLinkVisible());
        page.checkIssueActivityPresentedForQA5();

        page.setIssueKeyFilter("qa-4");
        bindPageAndSetJira();

        // because commit contains both keys QA-4 and QA-5, so should be present on both issues' commit tabs
        page.checkIssueActivityPresentedForQA5();

        page.setIssueKeyFilter("qa-5");
        bindPageAndSetJira();

        page.checkIssueActivityPresentedForQA5();

        goToConfigPage().deleteAllOrganizations();

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
        addOrganization();
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