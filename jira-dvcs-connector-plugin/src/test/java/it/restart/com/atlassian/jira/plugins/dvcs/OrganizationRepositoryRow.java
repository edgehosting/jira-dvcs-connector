package it.restart.com.atlassian.jira.plugins.dvcs;

import org.openqa.selenium.By;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverCheckboxElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;

/**
 * Represents single row of {@link OrganizationDiv}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class OrganizationRepositoryRow extends WebDriverElement
{

    /**
     * Enable checkbox - with responsibility to enable repository.
     * 
     * @see #isEnabled()
     * @see #enable()
     */
    @ElementBy(xpath = "td[contains(concat(' ', @class, ' '), ' dvcs-autolink-repo ')]/input[@type='checkbox']")
    private WebDriverCheckboxElement enableCheckbox;

    /**
     * Synchronization button - fire synchronization process.
     */
    @ElementBy(xpath = "td[contains(concat(' ', @class, ' '), ' dvcs-sync-repo ')]//a")
    private PageElement synchronizationButton;

    /**
     * Synchronization icon - holds information if synchronization is currently in progress.
     */
    @ElementBy(xpath = "td[4]/div/a/span")
    private PageElement synchronizationIcon;

    /**
     * Synchronization message, holds information about synchronization state, e.g.: Exception message
     */
    @ElementBy(xpath = "td[3]/div")
    private PageElement synchronizationMessage;

    /**
     * Constructor.
     * 
     * @param by
     * @param parent
     */
    public OrganizationRepositoryRow(By by, WebDriverLocatable parent)
    {
        super(by, parent);
    }

    /**
     * Constructor.
     * 
     * @param parent
     * @param timeoutType
     */
    public OrganizationRepositoryRow(WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(parent, timeoutType);
    }

    /**
     * @return Is repository enabled?
     */
    public boolean isEnabled()
    {
        return enableCheckbox.isSelected();
    }

    /**
     * Enables repository.
     * 
     * @see #isEnabled()
     */
    public void enable()
    {
        enableCheckbox.check();
    }

    /**
     * Fires synchronization button.
     */
    public void synchronize()
    {
        synchronizationButton.click();
    }

    /**
     * @return True if synchronization is currently in progress.
     */
    public boolean isSyncing()
    {
        if (synchronizationIcon.isPresent())
        {
            return synchronizationIcon.getAttribute("class").contains("running");
        }

        return false;
    }
}
