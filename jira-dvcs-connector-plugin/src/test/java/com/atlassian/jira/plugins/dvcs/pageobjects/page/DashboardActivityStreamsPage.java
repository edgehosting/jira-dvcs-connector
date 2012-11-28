package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.fest.assertions.api.Assertions.*;

/**
 *
 */
public class DashboardActivityStreamsPage implements Page
{

    private JiraTestedProduct jira;

    @ElementBy(xpath = "//h3[text() = 'Activity Stream']")
    private PageElement activityStreamsGadgetTitleElm;

    @ElementBy(linkText = "QA-5")
    private PageElement linkIssueQAElm;

    @ElementBy(id = "filter-icon")
    private PageElement filterIconElm;

    @ElementBy(className = "CommitRowsMore")
    private PageElement moreFilesLink;

    @Override
    public String getUrl()
    {
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

    public boolean isMoreFilesLinkVisible()
    {
    	return moreFilesLink.isVisible();
    }

    public void checkIssueActivityPresentedForQA5()
    {
        Poller.waitUntilTrue("Expected acitivity at issue QA-5", linkIssueQAElm.timed().isVisible());
    }

    public void checkIssueActivityNotPresentedForQA5()
    {
        Poller.waitUntilFalse("Expected acitivity at issue QA-5", linkIssueQAElm.timed().isVisible());
    }

    private void showFilter(){
        assertThat(filterIconElm.isVisible()).isTrue();
        filterIconElm.click();
    }

    public void setIssueKeyFilter(String issueKey)
    {
        showFilter();

        WebElement addFilterLinkElm = jira.getTester().getDriver().findElement(By.className("add-filter-link"));
        jira.getTester().getDriver().waitUntilElementIsVisible(By.className("add-filter-link"));
        addFilterLinkElm.click();

        WebElement ruleSelectkElm = jira.getTester().getDriver().findElement(By.className("rule"));
        ruleSelectkElm.findElement(By.xpath("//option[text() = 'JIRA Issue Key']")).click();


        WebElement issueKeyInputElm = jira.getTester().getDriver().findElement(By.name("streams-issue-key-is"));
        issueKeyInputElm.clear();
        issueKeyInputElm.sendKeys(issueKey);

        WebElement submitBtnElm = jira.getTester().getDriver().findElement(By.xpath("//button[@class='submit']"));
        submitBtnElm.click();
    }
}
