package com.atlassian.jira.plugins.bitbucket.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import junit.framework.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 *
 */
public class DashboardActivityStreamsPage implements Page
{

    private JiraTestedProduct jira;

    @ElementBy(xpath = "//h3[text() = 'Activity Stream']")
    private PageElement activityStreamsGadgetTitleElm;

    @ElementBy(linkText = "QA-3")
    private PageElement linkIssueQA3Elm;

    @ElementBy(id = "filter-icon")
    private PageElement filterIconElm;

    @Override
    public String getUrl()
    {
//        return "/secure/Dashboard.jspa";
        return "/secure/admin/EditDefaultDashboard!default.jspa";
    }

    public void setJira(JiraTestedProduct jira)
    {
        this.jira = jira;
    }

    public boolean isActivityStreamsGadgetVisible()
    {
        return activityStreamsGadgetTitleElm.isVisible();
    }

    public void checkIssueActivityPresentedForQA3()
    {
        Poller.waitUntilTrue("Expected acitivity from user farmas at issue QA-5", linkIssueQA3Elm.timed().isVisible());
    }

    public void checkIssueActivityNotPresentedForQA3()
    {
        Poller.waitUntilFalse("Expected acitivity from user farmas at issue QA-5", linkIssueQA3Elm.timed().isVisible());
    }

    private void showFilter(){
        Assert.assertTrue(filterIconElm.isVisible());
        filterIconElm.click();
    }

    public void setIssueKeyFilter(String issueKey)
    {
        showFilter();

        WebElement addFilterLinkElm = this. jira.getTester().getDriver().findElement(By.className("add-filter-link"));
        addFilterLinkElm.click();

        WebElement ruleSelectkElm = jira.getTester().getDriver().findElement(By.className("rule"));
        ruleSelectkElm.findElement(By.xpath("//option[text() = 'JIRA Issue Key']")).setSelected();


        WebElement issueKeyInputElm = jira.getTester().getDriver().findElement(By.name("streams-issue-key-is"));
        issueKeyInputElm.clear();
        issueKeyInputElm.sendKeys(issueKey);

        WebElement submitBtnElm = jira.getTester().getDriver().findElement(By.xpath("//button[@class='submit']"));
        submitBtnElm.click();
    }
}
