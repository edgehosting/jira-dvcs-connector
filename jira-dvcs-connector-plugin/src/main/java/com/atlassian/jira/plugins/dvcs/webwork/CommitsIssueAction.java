package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;

import java.util.Date;

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
