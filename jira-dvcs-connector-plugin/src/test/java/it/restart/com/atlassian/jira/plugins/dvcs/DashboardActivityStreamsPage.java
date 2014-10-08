package it.restart.com.atlassian.jira.plugins.dvcs;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.query.Poller;
import org.openqa.selenium.By;

/**
 *
 */
public class DashboardActivityStreamsPage implements Page
{
    @ElementBy(xpath = "//h3[text() = 'Activity Stream']")
    private PageElement activityStreamsGadgetTitleElm;

    @ElementBy(className = "footer")
    private PageElement settingsDropdownDiv;

    @ElementBy(className = "CommitRowsMore")
    private PageElement moreFilesLink;

    @ElementBy(linkText = "QA-5")
    private PageElement linkIssueQAElm;

    @ElementBy(id = "10001")
    private PageElement rootElement;

    @ElementBy(id = "config-form")
    private PageElement configForm;

    @Override
    public String getUrl()
    {
        // this url is used to get to the main dashboard page only
        // the actual url for the activity stream widget is calculated based on the iframe src
        return "/secure/Dashboard.jspa";
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

    private void showFilter()
    {
        PageElement dropdown = settingsDropdownDiv.find(By.className("aui-dd-link"));
        dropdown.click();
        Poller.waitUntilTrue(settingsDropdownDiv.find(By.linkText("Edit")).timed().isVisible());
        PageElement editLink = settingsDropdownDiv.find(By.linkText("Edit"));
        editLink.click();
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

        PageElement submitBtnElm = configForm.find(By.className("submit"));
        submitBtnElm.click();
    }
}
