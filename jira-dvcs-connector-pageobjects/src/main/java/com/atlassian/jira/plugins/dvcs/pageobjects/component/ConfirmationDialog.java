package com.atlassian.jira.plugins.dvcs.pageobjects.component;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static org.hamcrest.Matchers.is;

/**
 * Represents the confirmation dialog
 *
 */
public class ConfirmationDialog extends WebDriverElement
{
    /**
     * Ok button
     * 
     * @see #isOkButtonEnabled()
     * @see #confirm()
     */
    @ElementBy(cssSelector = ".button-panel-button.button-panel-submit-button")
    private PageElement okButton;

    /**
     * Cancel button
     * 
     * @see #cancel()
     */
    @ElementBy(cssSelector = ".button-panel-link.button-panel-cancel-link")
    private PageElement cancelButton;

    /**
     * Dialog title
     * 
     * @see #getDialogTitle()
     */
    @ElementBy(className = "dialog-title")
    private PageElement dialogTitle;

    /**
     * Dialog body
     * 
     * @see #getDialogBody()
     */
    @ElementBy(className = "dialog-panel-body")
    private PageElement dialogBody;
    
    public ConfirmationDialog(By locator)
    {
        super(locator);
    }

    public ConfirmationDialog(By locator, TimeoutType timeoutType)
    {
        super(locator, timeoutType);
    }
    
    public ConfirmationDialog(By locator, WebDriverLocatable parent)
    {
        super(locator, parent);
    }
    
    public ConfirmationDialog(By locator, WebDriverLocatable parent, TimeoutType timeoutType)
    {
        super(locator, parent, timeoutType);
    }

    public ConfirmationDialog(WebDriverLocatable locatable, TimeoutType timeoutType)
    {
        super(locatable, timeoutType);
    }
    
    public boolean isOkButtonEnabled()
    {
        return okButton.isEnabled();
    }
    
    public String getDialogTitle()
    {
        return dialogTitle.getText();
    }
    
    public PageElement getDialogBody()
    {
        return dialogBody;
    }
    
    public void confirm()
    {
        okButton.click();
    }
    
    public void cancel()
    {
        cancelButton.click();
    }
    
    public void waitUntilVisible()
    {
        Poller.waitUntil(timed().isVisible(), is(false), by(60000));
    }
}
