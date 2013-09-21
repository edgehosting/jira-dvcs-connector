package it.restart.com.atlassian.jira.plugins.dvcs.page.account;

import javax.annotation.Nullable;
import javax.inject.Inject;

import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.query.Conditions;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.internal.seleniumemulation.ElementFinder;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverCheckboxElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.google.common.base.Predicate;

/**
 * Represents repository table row of {@link AccountsPageAccountRepository}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class AccountsPageAccountRepository extends WebDriverElement
{

    @Inject
    private WebDriver webDriver;

    @Inject
    private PageElementFinder elementFinder;
    /**
     * Enable checkbox - with responsibility to enable repository.
     * 
     * @see #isEnabled()
     * @see #enable()
     */
    @ElementBy(xpath = "td[contains(concat(' ', @class, ' '), ' dvcs-autolink-repo ')]/input[@type='checkbox']")
    private WebDriverCheckboxElement enableCheckbox;

    /**
     * Synchronization icon - holds information if synchronization is currently in progress.
     */
    @ElementBy(xpath = "td[4]/div/a/span")
    private PageElement synchronizationIcon;

    /**
     * Synchronization button - fire synchronization process.
     */
    @ElementBy(xpath = "td[contains(concat(' ', @class, ' '), ' dvcs-sync-repo ')]//a")
    private PageElement synchronizationButton;

    /**
     * Constructor.
     * 
     * @param locator
     * @param parent
     */
    public AccountsPageAccountRepository(By locator, WebDriverLocatable parent)
    {
        super(locator, parent);
    }

    /**
     * @return is repository enabled?
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
        if (!isEnabled())
        {
            enableCheckbox.check();
            new WebDriverWait(driver, 15).until(new Predicate<WebDriver>()
            {

                @Override
                public boolean apply(@Nullable WebDriver input)
                {
                    return synchronizationButton.isVisible();
                }

            });
        }
    }

    /**
     * @return True if synchronization is currently in progress.
     */
    public boolean isSyncing()
    {
        int attempts = 5;
        do
        {
            try
            {
                if (synchronizationIcon.isPresent())
                {
                    return synchronizationIcon.getAttribute("class").contains("running");
                } else
                {
                    return false;
                }
            } catch (StaleElementReferenceException e)
            {
                // nothing to do - retry

                // check maximal retries - prevention in front of deadlock
                // if maximal retry attempts was exceeds - throws exception - it should never happened
                if (attempts-- > 0)
                {
                    throw new RuntimeException("Unable to work with synchronization icon, it seems as detached from DOM!", e);
                }
            }

            // if StaleElementReferenceException was happened, it is necessary to wait for next attempt,
            // because element was already replaced via AJAX by new element
        } while (true);
    }

    /**
     * Fires synchronization button.
     */
    public void synchronize()
    {
        synchronizationButton.click();
        new WebDriverWait(driver, 15).until(new Predicate<WebDriver>()
        {

            @Override
            public boolean apply(@Nullable WebDriver input)
            {
                return !isSyncing();
            }

        });
    }

    /**
     * Fires full synchronization
     */
    public void fullSynchronize()
    {
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            //nop
        }
        String script = synchronizationButton.getAttribute("onclick");
        script = script.replace("event", "{shiftKey: true}");
        synchronizationButton.javascript().execute(script);
        ForceSyncDialog forceSyncDialog = elementFinder.find(By.xpath("//div[contains(concat(' ', @class, ' '), ' forceSyncDialog ')]"), ForceSyncDialog.class);
        forceSyncDialog.fullSync();
        new WebDriverWait(driver, 15).until(new Predicate<WebDriver>()
        {

            @Override
            public boolean apply(@Nullable WebDriver input)
            {
                return !isSyncing();
            }

        });
    }

    public static class ForceSyncDialog extends WebDriverElement
    {
        @ElementBy(xpath = "//a[@class='aui-button']")
        private PageElement fullSyncButton;

        public ForceSyncDialog(final By locator)
        {
            super(locator);
        }

        public ForceSyncDialog(final By locator, final TimeoutType timeoutType)
        {
            super(locator, timeoutType);
        }

        public ForceSyncDialog(final By locator, final WebDriverLocatable parent)
        {
            super(locator, parent);
        }

        public ForceSyncDialog(final By locator, final WebDriverLocatable parent, final TimeoutType timeoutType)
        {
            super(locator, parent, timeoutType);
        }

        public ForceSyncDialog(final WebDriverLocatable locatable, final TimeoutType timeoutType)
        {
            super(locatable, timeoutType);
        }

        public void fullSync()
        {
            fullSyncButton.click();
        }
    }
}
