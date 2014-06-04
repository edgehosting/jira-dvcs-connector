package it.restart.com.atlassian.jira.plugins.dvcs.page.account;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryList;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.RepositoriesLocalRestpoint;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.WebDriverCheckboxElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import com.google.common.base.Predicate;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import javax.annotation.Nullable;
import javax.inject.Inject;

/**
 * Represents repository table row of {@link AccountsPageAccountRepository}.
 *
 * @author Stanislav Dvorscak
 *
 */
public class AccountsPageAccountRepository extends WebDriverElement
{

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

    @ElementBy(xpath = "td[3]/div")
    private PageElement message;

    @ElementBy(xpath = "td[@class='dvcs-org-reponame']/span[starts-with(@id,'error_status_icon_')]")
    private PageElement warningIcon;

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
     * Constructor
     *
     * @param locator
     * @param timeoutType
     */
    public AccountsPageAccountRepository(By locator, TimeoutType timeoutType)
    {
        super(locator, timeoutType);
    }

    /**
     * Constructor
     *
     * @param locatable
     * @param timeoutType
     */
    public AccountsPageAccountRepository(WebDriverLocatable locatable, TimeoutType timeoutType)
    {
        super(locatable, timeoutType);
    }

    /**
     * @return is repository enabled?
     */
    @Override
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
        enable(false);
    }

    public void enable(boolean noAdminPermission)
    {
        if (!isEnabled())
        {
            enableCheckbox.check();

            if (noAdminPermission)
            {
                // check that dialog appears
                LinkingRepositoryDialog linkingRepositoryDialog = elementFinder.find(By.id("dvcs-postcommit-hook-registration-dialog"), LinkingRepositoryDialog.class);
                linkingRepositoryDialog.clickOk();
            }

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
        RepositoryList repositories = new RepositoriesLocalRestpoint().getRepositories();
        for (Repository repository : repositories.getRepositories()) {
            if (repository.getSync() != null && !repository.getSync().isFinished()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Fires synchronization button.
     */
    public void synchronize()
    {
        synchronizationButton.click();
        try
        {
            Thread.sleep(5000l);
        } catch (InterruptedException e)
        {
            // ignore
        }
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
        try
        {
            Thread.sleep(1000l);
        } catch (InterruptedException e)
        {
            // ignore
        }
        new WebDriverWait(driver, 15).until(new Predicate<WebDriver>()
        {

            @Override
            public boolean apply(@Nullable WebDriver input)
            {
                return !isSyncing();
            }

        });
    }

    public String getMessage()
    {
        return message.getText();
    }

    public boolean hasWarning()
    {
        return warningIcon.hasClass("admin_permission") && warningIcon.hasClass("aui-iconfont-warning") && warningIcon.isVisible();
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

    /**
     * Page class for linking repository dialog
     *
     */
    public static class LinkingRepositoryDialog extends WebDriverElement
    {
        @ElementBy (xpath = "//div[@class='dialog-button-panel']/button")
        private PageElement okButton;

        public LinkingRepositoryDialog(final By locator)
        {
            super(locator);
        }

        public LinkingRepositoryDialog(final By locator, final TimeoutType timeoutType)
        {
            super(locator, timeoutType);
        }

        public LinkingRepositoryDialog(final By locator, final WebDriverLocatable parent)
        {
            super(locator, parent);
        }

        public LinkingRepositoryDialog(final By locator, final WebDriverLocatable parent, final TimeoutType timeoutType)
        {
            super(locator, parent, timeoutType);
        }

        public LinkingRepositoryDialog(final WebDriverLocatable locatable, final TimeoutType timeoutType)
        {
            super(locatable, timeoutType);
        }

        public void clickOk()
        {
            okButton.click();
        }
    }
}
