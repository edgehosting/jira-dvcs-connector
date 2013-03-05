package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

public class IssuePagePullRequestTabActivityComment extends IssuePagePullRequestTabActivity
{

    /**
     * Constructor.
     * 
     * @param parent
     * @param timeoutType
     */
    public IssuePagePullRequestTabActivityComment(WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(parent, timeoutType);
    }

}
