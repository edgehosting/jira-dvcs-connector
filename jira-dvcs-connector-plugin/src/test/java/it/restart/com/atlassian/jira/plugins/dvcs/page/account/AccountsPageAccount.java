package it.restart.com.atlassian.jira.plugins.dvcs.page.account;

import javax.annotation.Nullable;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElement;
import com.atlassian.pageobjects.elements.WebDriverElement;
import com.google.common.base.Predicate;

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
        GIT_HUB("githubLogo"), GIT_HUB_ENTERPRISE("githubeLogo");

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

    /**
     * Reference to "Controls" button.
     */
    @ElementBy(xpath = ".//li[contains(concat(' ', @class, ' '), 'dvcs-organization-controls-tool')]//a")
    private PageElement controlsButton;

    /**
     * Reference to "Controls" dialog, which appeared after {@link #controlsButton} fire.
     */
    @ElementBy(xpath = ".//li[contains(concat(' ', @class, ' '), 'dvcs-organization-controls-tool')]//ul")
    private AccountsPageAccountControlsDialog controlsDialog;

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
     * Refreshes repositories of this account.
     */
    public void refresh()
    {
        controlsButton.click();
        controlsDialog.refresh();
        new WebDriverWait(driver, 15).until(new Predicate<WebDriver>()
        {

            @Override
            public boolean apply(@Nullable WebDriver input)
            {
                return controlsButton.isVisible();
            }

        });
    }

}
