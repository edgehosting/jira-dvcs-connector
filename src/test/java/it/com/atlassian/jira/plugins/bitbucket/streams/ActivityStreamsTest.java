package it.com.atlassian.jira.plugins.bitbucket.streams;

import com.atlassian.jira.plugins.bitbucket.pageobjects.page.BitBucketConfigureRepositoriesPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.DashboardActivityStreamsPage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.atlassian.webdriver.jira.page.DashboardPage;
import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
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

    @SuppressWarnings("unchecked")
    @Before
    public void loginToJira()
    {
        jira.gotoLoginPage().loginAsSysAdmin(DashboardPage.class);

        BitBucketConfigureRepositoriesPage configureRepos = goToRepositoriesConfigPage();
        configureRepos.deleteAllRepositories();
        configureRepos.addPublicRepoToProjectSuccessfully("QA", "https://bitbucket.org/farmas/testrepo-qa");

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
    }

    private void bindPageAndSetJira()
    {
        page = jira.getPageBinder().bind(DashboardActivityStreamsPage.class);
        page.setJira(jira);
    }
}