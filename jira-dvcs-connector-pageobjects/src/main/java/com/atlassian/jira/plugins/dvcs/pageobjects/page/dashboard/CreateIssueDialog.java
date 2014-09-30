package com.atlassian.jira.plugins.dvcs.pageobjects.page.dashboard;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

/**
 * Create issue dialog, which is displayed on dashboard.
 * 
 * @see DashboardPage#getCreateIssueDialog()
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class CreateIssueDialog extends WebDriverElement
{

    /**
     * Reference to "Summary" text field.
     */
    @ElementBy(id = "summary")
    private PageElement summaryTextField;

    /**
     * Reference to "Create" button.
     */
    @ElementBy(id = "create-issue-submit")
    private PageElement createButton;

    /**
     * Constructor.
     * 
     * @param locator
     * @param timeoutType
     */
    public CreateIssueDialog(By locator, TimeoutType timeoutType)
    {
        super(locator, timeoutType);
    }

    /**
     * Fills form with provided data.
     * 
     * @param summary
     *            of issue
     */
    public void fill(String summary)
    {
        summaryTextField.type(summary);
    }

    /**
     * Fires "Create" button.
     */
    public void create()
    {
        createButton.click();
    }

}
