package com.atlassian.jira.plugins.dvcs.pageobjects.page;

import com.atlassian.jira.plugins.dvcs.pageobjects.component.BitBucketIssuePanel;
import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.PageBinder;

import javax.inject.Inject;

/**
 * Represents the JIRA view issue page
 */
public class JiraViewIssuePage implements Page
{
    @Inject
    PageBinder pageBinder;

    private final String issueKey;

    public JiraViewIssuePage(String issueKey)
    {
        this.issueKey = issueKey;
    }

    @Override
	public String getUrl()
    {
        return "/browse/" + issueKey;
    }

    /**
     * Opens the bitbucket panel
     * @return BitBucketIssuePanel
     */
    public BitBucketIssuePanel openBitBucketPanel()
    {
        return pageBinder.bind(BitBucketIssuePanel.class).open();
    }
}
