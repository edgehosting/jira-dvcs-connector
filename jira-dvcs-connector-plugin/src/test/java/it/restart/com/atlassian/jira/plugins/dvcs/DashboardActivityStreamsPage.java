package it.restart.com.atlassian.jira.plugins.dvcs;

import static org.fest.assertions.api.Assertions.assertThat;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;

/**
 *
 */
public class DashboardActivityStreamsPage implements Page
{
    @ElementBy(xpath = "//h3[text() = 'Activity Stream']")
    private PageElement activityStreamsGadgetTitleElm;

    @ElementBy(id = "filter-icon")
    private PageElement filterIconElm;

    @ElementBy(className = "CommitRowsMore")
    private PageElement moreFilesLink;

    @ElementBy(linkText = "QA-5")
    private PageElement linkIssueQAElm;

    @ElementBy(id = "10001")
    private PageElement rootElement;

    @Override
    public String getUrl()
    {
        return "/secure/admin/EditDefaultDashboard!default.jspa";
    }

    public void checkIssueActivityPresentedForQA5()
    {
        Poller.waitUntilTrue("Expected acitivity at issue QA-5", linkIssueQAElm.timed().isVisible());
    }

    public void checkIssueActivityNotPresentedForQA5()
    {
        Poller.waitUntilFalse("Expected acitivity at issue QA-5", linkIssueQAElm.timed().isVisible());
    }

    public boolean isActivityStreamsGadgetVisible()
    {
        return activityStreamsGadgetTitleElm.isVisible();
    }

    public boolean isMoreFilesLinkVisible()
    {
    	return moreFilesLink.isVisible();
    }

    private void showFilter(){
        assertThat(filterIconElm.isVisible()).isTrue();
        filterIconElm.click();
    }

    public void setIssueKeyFilter(String issueKey)
    {
        showFilter();

        PageElement addFilterLinkElm = rootElement.find(By.className("add-filter-link"));

        Poller.waitUntilTrue( rootElement.find(By.className("add-filter-link")).timed().isVisible());
        addFilterLinkElm.click();

        PageElement ruleSelectkElm = rootElement.find(By.className("rule"));
        ruleSelectkElm.find(By.xpath("//option[text() = 'JIRA Issue Key']")).click();


        PageElement issueKeyInputElm = rootElement.find(By.name("streams-issue-key-is"));
        issueKeyInputElm.clear();
        issueKeyInputElm.type(issueKey);

        PageElement submitBtnElm = rootElement.find(By.xpath("//button[@class='submit']"));
        submitBtnElm.click();
    }
}
