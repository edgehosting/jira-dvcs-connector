package it.com.atlassian.jira.plugins.bitbucket.streams;

import it.com.atlassian.jira.plugins.bitbucket.BitBucketBaseTest.AnotherLoginPage;
import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.atlassian.jira.plugins.bitbucket.pageobjects.page.BitBucketConfigureRepositoriesPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.DashboardActivityStreamsPage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.atlassian.webdriver.jira.page.DashboardPage;

/**
 *
 */
public class ActivityStreamsTest
{
    private static final String TEST_PUBLIC_REPO_URL = "https://bitbucket.org/jirabitbucketconnector/public-hg-repo";

    protected static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    private DashboardActivityStreamsPage page;

    @Before
    public void loginToJira()
    {
        jira.getPageBinder().override(LoginPage.class, AnotherLoginPage.class);
        jira.getPageBinder().navigateToAndBind(AnotherLoginPage.class).loginAsSysAdmin(DashboardPage.class);

        BitBucketConfigureRepositoriesPage configureRepos = goToRepositoriesConfigPage();
        configureRepos.deleteAllRepositories();
        configureRepos.addRepoToProjectSuccessfully("QA", TEST_PUBLIC_REPO_URL);

        goToDashboardPage();
    }

    @After
    public void logout()
    {
        jira.getTester().getDriver().manage().deleteAllCookies();
    }

    protected BitBucketConfigureRepositoriesPage goToRepositoriesConfigPage()
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

    @Test
    public void testActivityPresentedForQA5()
    {
        Assert.assertTrue("Activity streams gadget expected at dashboard page!", page.isActivityStreamsGadgetVisible());

        WebElement iframeElm = jira.getTester().getDriver().getDriver().findElement(By.id("gadget-10001"));
        String iframeSrc = iframeElm.getAttribute("src");
        jira.getTester().gotoUrl(iframeSrc);
        bindPageAndSetJira();

        page.checkIssueActivityPresentedForQA3();

        page.setIssueKeyFilter("qa-4");
        bindPageAndSetJira();

        page.checkIssueActivityNotPresentedForQA3();

        page.setIssueKeyFilter("qa-3");
        bindPageAndSetJira();

        page.checkIssueActivityPresentedForQA3();

        goToRepositoriesConfigPage().deleteAllRepositories();

        goToDashboardPage();
        bindPageAndSetJira();

        page.checkIssueActivityNotPresentedForQA3();
    }

    private void bindPageAndSetJira()
    {
        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);
        page.setJira(jira);
    }
}