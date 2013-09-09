package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * Represents actions dialog, displayed when {@link IssuesPageIssueRow#actionLink} was performed.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuesPageIssueRowActionsDialog extends WebDriverElement
{

    /**
     * Reference to "Delete" action link.
     */
    @ElementBy(xpath = "//ul/li/a[contains(concat(' ', @class, ' '), 'issueaction-delete-issue')]")
    private PageElement deleteLink;

    /**
     * Constructor.
     * 
     * @param locator
     * @param parent
     * @param timeoutType
     */
    public IssuesPageIssueRowActionsDialog(By locator, WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(locator, parent, timeoutType);
    }

    /**
     * Fires delete action link.
     */
    public void delete()
    {
        deleteLink.click();
    }

}