package it.restart.com.atlassian.jira.plugins.dvcs.page.issue;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverCheckboxElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * Represents "More Criteria" dialog of {@link IssuesPage}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssuesPageMoreCriteriaDialog extends WebDriverElement
{

    /**
     * Text field for filter interested criteria.
     */
    @ElementBy(id = "criteria-input")
    private PageElement criteriaTextField;

    /**
     * Constructor.
     * 
     * @param locator
     * @param timeoutType
     */
    public IssuesPageMoreCriteriaDialog(By locator, TimeoutType timeoutType)
    {
        super(locator, timeoutType);
    }

    /**
     * Adds provided search criteria to search form.
     * 
     * @param fieldKey
     *            key of criteria field
     * @param fieldName
     *            name of criteria field
     */
    public void add(String fieldKey, String fieldName)
    {
        criteriaTextField.type(fieldName);

        WebDriverCheckboxElement checkbox = find(By.xpath("//input[@type='checkbox'][@value='" + fieldKey + "']"),
                WebDriverCheckboxElement.class);
        if (!checkbox.isSelected())
        {
            checkbox.select();
        } else
        {
            checkbox.uncheck();
            checkbox.select();
        }
    }

}
