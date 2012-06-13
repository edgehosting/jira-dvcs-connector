package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.Date;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;

public class CommitsIssueAction implements IssueAction
{
    private final String htmlContent;
    private final Date timestamp;

    public CommitsIssueAction(String changesetAsHtml, Date timestamp)
    {
        htmlContent = changesetAsHtml;
        this.timestamp = timestamp;
    }

    @Override
    public String getHtml()
    {
        return htmlContent;
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
