package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

/**
 * Controls dialog of {@link AccountsPageAccount}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class AccountsPageAccountControlsDialog extends WebDriverElement
{

    /**
     * Reference to "Refresh List" link.
     */
    @ElementBy(linkText = "Refresh list")
    private PageElement refreshLink;

    /**
     * Reference "Reset OAuth Settings" link.
     */
    @ElementBy(linkText = "Reset OAuth Settings")
    private PageElement regenerateLink;

    /**
     * Constructor.
     *
     * @param locator
     * @param parent
     * @param timeoutType
     */
    public AccountsPageAccountControlsDialog(By locator, WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(locator, parent, timeoutType);
    }

    /**
     * Constructor.
     *
     * @param locator
     * @param parent
     */
    public AccountsPageAccountControlsDialog(By locator, WebDriverLocatable parent)
    {
        super(locator, parent);
    }

    /**
     * Constructor.
     *
     * @param locator
     */
    public AccountsPageAccountControlsDialog(By locator)
    {
        super(locator);
    }

    /**
     * Refreshes repositories list of account.
     */
    public void refresh()
    {
        refreshLink.click();
    }

    /**
     * Regenerates account OAuth.
     */
    public void regenerate()
    {
        regenerateLink.click();
    }

}
