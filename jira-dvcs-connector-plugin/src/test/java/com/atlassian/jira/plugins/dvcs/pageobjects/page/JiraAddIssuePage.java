package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import javax.inject.Inject;

import com.atlassian.jira.plugins.dvcs.util.PageElementUtils;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.webdriver.AtlassianWebDriver;

/**
 * @author Martin Skurla
 */
public class JiraAddIssuePage implements Page
{
    @Inject
    AtlassianWebDriver atlassianWebDriver;

    @Inject
    PageBinder pageBinder;

    @ElementBy(id = "issue-create-submit")
    PageElement createIssueButton;


    @Override
    public String getUrl()
    {
        return "/secure/CreateIssue!default.jspa";
    }


    public void createIssue()
    {
        createIssueButton.click();
        PageElementUtils.waitUntilPageUrlDoesNotContain(atlassianWebDriver, "!default");

        JiraCreateIssuePage jiraCreateIssuePage = pageBinder.bind(JiraCreateIssuePage.class);
        jiraCreateIssuePage.createIssue();
    }


    public static class JiraCreateIssuePage implements Page
    {
        @ElementBy(id = "summary")
        PageElement summaryInput;

        @ElementBy(id = "issue-create-submit")
        PageElement createIssueButton;

        @Override
        public String getUrl() {
            return "/jira/secure/CreateIssue.jspa";
        }

        private void createIssue()
        {
            summaryInput.type("Missing commits fix demonstration");

            createIssueButton.click();
        }
    }
}
