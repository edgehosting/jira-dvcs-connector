package com.atlassian.jira.plugins.dvcs.pageobjects.page.account;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.PageElementFinder;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.atlassian.pageobjects.elements.WebDriverLocatable;
import com.atlassian.pageobjects.elements.query.Poller;
import com.atlassian.pageobjects.elements.timeout.TimeoutType;
import org.openqa.selenium.By;

import java.util.List;
import javax.inject.Inject;

import static com.atlassian.pageobjects.elements.query.Poller.by;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;

/**
 * Container of single account of {@link AccountsPage}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class AccountsPageAccount extends WebDriverElement
{

    /**
     * Type of account.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    public enum AccountType
    {
        /**
         * GitHub account type.
         */
        GIT_HUB("githubLogo"), GIT_HUB_ENTERPRISE("githubeLogo"),

        /**
         * Bitbucket account type.
         */
        BITBUCKET("bitbucketLogo");

        /**
         * @see #getLogoClassName()
         */
        private String logoClassName;

        /**
         * Constructor.
         * 
         * @param logoClassName
         *            {@link #getLogoClassName()}
         */
        private AccountType(String logoClassName)
        {
            this.logoClassName = logoClassName;
        }

        /**
         * @return CSS class name of logo
         */
        public String getLogoClassName()
        {
            return logoClassName;
        }
    }

    @Inject
    private PageElementFinder elementFinder;
    
    /**
     * Reference to "Controls" button.
     */
    @ElementBy(xpath = ".//button[contains(concat(' ', @class, ' '), ' aui-dropdown2-trigger ')]")
    private PageElement controlsButton;

    /**
     * Reference to {@link AccountsPageAccountOAuthDialog}.
     * 
     * @see #regenerate()
     */
    @ElementBy(xpath = "//div[contains(concat(' ', @class, ' '), ' dialog-components ')]")
    private AccountsPageAccountOAuthDialog oAuthDialog;

    /**
     * @see #isOnDemand()
     */
    @ElementBy(xpath = ".//span[@title='OnDemand']")
    private PageElement onDemandDecorator;

    /**
     * Constructor.
     * 
     * @param locator
     */
    public AccountsPageAccount(By locator)
    {
        super(locator);
    }

    /**
     * Constructor.
     * 
     * @param locatable
     * @param timeoutType
     */
    public AccountsPageAccount(WebDriverLocatable locatable, TimeoutType timeoutType)
    {
        super(locatable, timeoutType);
    }

    /**
     * Resolves repository for provided name.
     * 
     * @param repositoryName
     *            name of repository
     * @return resolved repository
     */
    public AccountsPageAccountRepository getRepository(String repositoryName)
    {
        return find(By.xpath("table/tbody/tr/td[@class='dvcs-org-reponame']/a[text()='" + repositoryName + "']/ancestor::tr"),
                AccountsPageAccountRepository.class);
    }

    /**
     * Resolves repository for provided name.
     *
     * @return resolved repositories
     */
    public List<AccountsPageAccountRepository> getRepositories()
    {
        return findAll(By.xpath("table/tbody/tr[contains(concat(' ', @class, ' '), ' dvcs-repo-row ')]"),
                AccountsPageAccountRepository.class);
    }

    /**
     * @return True if this account is consider to be OnDemand account.
     */
    public boolean isOnDemand()
    {
        return onDemandDecorator.isPresent() && onDemandDecorator.isVisible();
    }

    /**
     * Refreshes repositories of this account.
     */
    public void refresh()
    {
        controlsButton.click();
        findControlDialog().refresh();
        // wait for popup to show up
        try
        {
            Poller.waitUntilTrue(find(By.id("refreshing-account-dialog")).timed().isVisible());
        } catch (AssertionError e)
        {
            // ignore, the refresh was probably very quick and the popup has been already closed.
        }
        Poller.waitUntil(find(By.id("refreshing-account-dialog")).timed().isVisible(), is(false), by(1200000));
        assertFalse(find(By.id("refreshing-account-dialog")).isVisible());
    }

    /**
     * Regenerates account OAuth.
     * 
     * @return OAuth dialog
     */
    public AccountsPageAccountOAuthDialog regenerate()
    {
        controlsButton.click();
        findControlDialog().regenerate();
        return oAuthDialog;
    }

    public AccountsPageAccountRepository enableRepository(String repositoryName, boolean noAdminPermission)
    {
        AccountsPageAccountRepository repository = getRepository(repositoryName);

        repository.enable(noAdminPermission);

        return repository;
    }
    
    /**
     * @return "Controls" dialog, which appeared after {@link #controlsButton} fire.
     */
    private AccountsPageAccountControlsDialog findControlDialog()
    {
        String dropDownMenuId = controlsButton.getAttribute("aria-owns");
        return elementFinder.find(By.id(dropDownMenuId), AccountsPageAccountControlsDialog.class);
    }

    /**
     * Synchronizes the repository with the given name
     *
     * @param repositoryName name of the repository to be synchronized
     * @return page object of the repository
     */
    public AccountsPageAccountRepository synchronizeRepository(String repositoryName)
    {
        AccountsPageAccountRepository repository = getRepository(repositoryName);

        if (!repository.isEnabled())
        {
            repository.enable();
        }

        repository.synchronize();

        return repository;
    }

    /**
     * Full synchronizes the repository with the given name
     *
     * @param repositoryName name of the repository to be synchronized
     * @return page object of the repository
     */
    public AccountsPageAccountRepository fullSynchronizeRepository(String repositoryName)
    {
        AccountsPageAccountRepository repository = getRepository(repositoryName);
        if (!repository.isEnabled())
        {
            repository.enable();
        }
        repository.fullSynchronize();

        return repository;
    }

    /**
     * Synchronize the repositories with the given names
     *
     * @param repositoryNames names of the repositories to be synchronized
     */
    public void synchronizeRepositories(String... repositoryNames)
    {
        for (String repositoryName : repositoryNames)
        {
            AccountsPageAccountRepository repository = getRepository(repositoryName);
            repository.enable();
            repository.synchronizeWithNoWait();
        }
    }
}
