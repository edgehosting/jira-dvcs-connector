package com.atlassian.jira.plugins.dvcs.pageobjects.page.dashboard;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;

/**
 * Abstraction of Jira dashboard page.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class DashboardPage implements Page
{

    /**
     * Creates issue link.
     */
    @ElementBy(id = "create_link")
    private PageElement createIssueLink;

    /**
     * @see #getCreateIssueDialog()
     */
    @ElementBy(id = "create-issue-dialog")
    private CreateIssueDialog createIssueDialog;

    /**
     * Fire "Creates Issue" link.
     * 
     * @see #getCreateIssueDialog()
     */
    public void createIssue()
    {
        createIssueLink.click();
    }

    /**
     * @return Returns {@link CreateIssueDialog} which is appeared after {@link #createIssue()}.
     * 
     * @see #createIssue()
     */
    public CreateIssueDialog getCreateIssueDialog()
    {
        return createIssueDialog;
    }

    @Override
    public String getUrl()
    {
        return "/secure/Dashboard.jspa";
    }

}
