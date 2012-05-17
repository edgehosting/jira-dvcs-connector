package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.issue.tabpanels.GenericMessageAction;

import java.util.Date;

public class CommitsIssueAction extends GenericMessageAction
{
    private Date timestamp;

    public CommitsIssueAction(String changesetAsHtml, Date timestamp)
    {
        super(changesetAsHtml);
        this.timestamp = timestamp;
    }

    @Override
    public Date getTimePerformed()
    {
        return timestamp;
    }

    @Override
    public boolean isDisplayActionAllTab()
    {
        return true;
    }
}
