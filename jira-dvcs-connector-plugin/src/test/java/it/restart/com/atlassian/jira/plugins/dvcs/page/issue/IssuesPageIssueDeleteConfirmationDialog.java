package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * Represents delete issue confirmation dialog.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuesPageIssueDeleteConfirmationDialog extends WebDriverElement
{

    /**
     * Reference to delete button.
     */
    @ElementBy(id = "delete-issue-submit")
    private PageElement deleteButton;

    /**
     * Constructor.
     * 
     * @param locator
     * @param timeoutType
     */
    public IssuesPageIssueDeleteConfirmationDialog(By locator, TimeoutType timeoutType)
    {
        super(locator, timeoutType);
    }

    /**
     * Confirm deletion.
     */
    public void confirm()
    {
        deleteButton.click();
    }

}
