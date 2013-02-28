package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * "Summary criteria" dialog of {@link IssuesPage}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuesPageSummaryCriteriaDialog extends WebDriverElement
{

    /**
     * Reference to "Summary" text field.
     */
    @ElementBy(name = "summary")
    private PageElement summaryTextField;

    /**
     * Reference to "Update" button.
     */
    @ElementBy(xpath = "//input[@type='submit'][@value='Update']")
    private PageElement updateButton;

    /**
     * Constructor.
     * 
     * @param locator
     * @param timeoutType
     */
    public IssuesPageSummaryCriteriaDialog(By locator, TimeoutType timeoutType)
    {
        super(locator, timeoutType);
    }

    /**
     * Fills this dialog.
     * 
     * @param summary
     *            of issue
     */
    public void fill(String summary)
    {
        summaryTextField.clear();
        summaryTextField.type(summary);
    }

    /**
     * Fires "Update" button of this form.
     */
    public void update()
    {
        updateButton.click();
    }

}
