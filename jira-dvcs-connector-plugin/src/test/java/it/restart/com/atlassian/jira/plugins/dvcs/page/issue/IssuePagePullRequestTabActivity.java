package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

public class IssuePagePullRequestTabActivity extends WebDriverElement
{

    /**
     * Constructor.
     * 
     * @param parent
     * @param timeoutType
     */
    public IssuePagePullRequestTabActivity(WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(parent, timeoutType);
    }

}
