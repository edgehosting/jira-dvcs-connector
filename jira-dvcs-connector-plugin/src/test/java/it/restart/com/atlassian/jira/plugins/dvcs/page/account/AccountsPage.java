package it.restart.com.atlassian.jira.plugins.dvcs.page.account;

import com.atlassian.pageobjects.Page;
import com.atlassian.pageobjects.elements.ElementBy;
import com.atlassian.pageobjects.elements.PageElementFinder;
import org.openqa.selenium.By;

import javax.inject.Inject;

/**
 * Holds available DVCS accounts.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class AccountsPage implements Page
{

    /**
     * Injected {@link PageElementFinder} dependency.
     */
    @Inject
    private PageElementFinder pageElementFinder;

    /**
     * Reference to all accounts.
     */
    @ElementBy(className = "dvcs-orgdata-container", pageElementClass = AccountsPageAccount.class)
    private Iterable<AccountsPageAccount> accounts;

    /**
     * @return All accounts available on page.
     */
    public Iterable<AccountsPageAccount> getAccounts()
    {
        return accounts;
    }

    /**
     * Constructor.
     * 
     * @param accountType
     *            type of account
     * @param accountName
     *            name of account
     * @return founded account element
     */
    public AccountsPageAccount getAccount(AccountsPageAccount.AccountType accountType, String accountName)
    {
        return pageElementFinder.find(
                By.xpath("//h4[contains(concat(' ', @class, ' '), '" + accountType.getLogoClassName() + "')]/a[text() = '" + accountName
                        + "']/ancestor::div[contains(concat(' ', @class, ' '), 'dvcs-orgdata-container')]"), AccountsPageAccount.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUrl()
    {
        return "/secure/admin/ConfigureDvcsOrganizations!default.jspa";
    }

}
