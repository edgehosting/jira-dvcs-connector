package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * Represents single table row of {@link IssuesPage}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuesPageIssueRow extends WebDriverElement
{

    @ElementBy(xpath = "//td[contains(concat(' ', @class, ' '), 'issuekey')]/a")
    private PageElement issueKeyCellLink;

    /**
     * Reference to "Action" link.
     */
    @ElementBy(xpath = "//td[contains(concat(' ', @class, ' '), 'issue_actions')]//a")
    PageElement actionLink;

    /**
     * Reference to actions dialog, provided when {@link #actionLink} was performed.
     */
    @ElementBy(xpath = "//div[@class='ajs-layer box-shadow active']")
    private IssuesPageIssueRowActionsDialog actionsDialog;

    /**
     * Constructor.
     * 
     * @param parent
     * @param timeoutType
     */
    public IssuesPageIssueRow(WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(parent, timeoutType);
    }

    /**
     * Constructor.
     * 
     * @param locator
     * @param parent
     * @param timeoutType
     */
    public IssuesPageIssueRow(By locator, WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(locator, parent, timeoutType);
    }

    /**
     * @return Returns issue key of issue represented by this row.
     */
    public String getIssueKey()
    {
        return issueKeyCellLink.getText().trim();
    }

    /**
     * Perform delete actions on this issue row.
     */
    public void delete()
    {
        actionLink.click();
        actionsDialog.delete();
    }
}
