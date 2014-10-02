package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.ondemand.JsonFileBasedAccountsConfigProvider;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount.AccountType;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccountOAuthDialog;
import com.atlassian.jira.plugins.dvcs.util.PasswordUtil;
import com.atlassian.pageobjects.TestedProductFactory;
import com.google.common.base.Predicate;
import it.com.atlassian.jira.plugins.dvcs.DvcsWebDriverTestCase;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketGrantAccessPage;
import junit.framework.Assert;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.json.JSONException;
import org.json.JSONWriter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Iterator;
import javax.annotation.Nullable;

/**
 * Tests integrated accounts functionality.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IntegratedAccountsTest extends DvcsWebDriverTestCase
{

    private static final String BB_ACCOUNT_NAME = "jirabitbucketconnector";
    /**
     * Name of tested account.
     */
    private static final String ACCOUNT_NAME = BB_ACCOUNT_NAME;

    /**
     * Access Jira instance.
     */
    private static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);

    /**
     * Represents information of an integrated accounts.
     * 
     * @author Stanislav Dvorscak
     * 
     */
    private static final class IntegratedAccount
    {

        /**
         * @see #IntegratedAccount(String, String, String)
         */
        public final String name;

        /**
         * @see #IntegratedAccount(String, String, String)
         */
        public final String key;

        /**
         * @see #IntegratedAccount(String, String, String)
         */
        public final String secret;

        /**
         * Constructor.
         * 
         * @param name
         *            name of account
         * @param key
         *            appropriate OAuth key of account
         * @param secret
         *            appropriate OAuth secret of account
         */
        public IntegratedAccount(String name, String key, String secret)
        {
            this.name = name;
            this.key = key;
            this.secret = secret;
        }

    }

    /**
     * OAuth used as original - first/initial ...
     */
    private OAuth oAuthOriginal;

    /**
     * OAuth used as changed/new.
     */
    private OAuth oAuthNew;

    /**
     * Path to ondemand.properties.
     */
    private String onDemandConfigurationPath;

    /**
     * Prepares common environment.
     */
    @BeforeClass
    public void beforeTest()
    {
        // log in to JIRA
        new JiraLoginPageController(jira).login();
        new MagicVisitor(jira).visit(BitbucketLoginPage.class).doLogin(BB_ACCOUNT_NAME, PasswordUtil.getPassword(BB_ACCOUNT_NAME));

        oAuthOriginal = new MagicVisitor(jira).visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).addConsumer();
        oAuthNew = new MagicVisitor(jira).visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).addConsumer();

        onDemandConfigurationPath = System.getProperty( //
                JsonFileBasedAccountsConfigProvider.ENV_ONDEMAND_CONFIGURATION, // environment customization
                JsonFileBasedAccountsConfigProvider.ENV_ONDEMAND_CONFIGURATION_DEFAULT // default value
                );
    }

    /**
     * Destroys common environment.
     */
    @AfterClass(alwaysRun = true)
    public void afterTestAlways()
    {
        new MagicVisitor(jira).visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).removeConsumer(oAuthOriginal.applicationId);
        new MagicVisitor(jira).visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).removeConsumer(oAuthNew.applicationId);
    }

    /**
     * Prepares test environment.
     */
    @BeforeMethod
    public void beforeMethod()
    {
        removeAllIntegratedAccounts();
        new RepositoriesPageController(jira).getPage().deleteAllOrganizations();
    }

    /**
     * Destroys test environment.
     */
    @AfterMethod(alwaysRun = true)
    public void afterMethod()
    {
        removeAllIntegratedAccounts();
        new RepositoriesPageController(jira).getPage().deleteAllOrganizations();
    }

    /**
     * Tests that OAuth of an account is regenerated/changed correctly.
     */
    @Test
    public void testEditOAuth()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jira);
        repositoriesPageController.addOrganization(
                it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController.AccountType.BITBUCKET, ACCOUNT_NAME,
                new OAuthCredentials(oAuthOriginal.key, oAuthOriginal.secret), false);

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.regenerate().regenerate(oAuthNew.key, oAuthNew.secret);

        if (jira.getTester().getDriver().getCurrentUrl().startsWith("https://bitbucket.org/api/"))
        {
            jira.getPageBinder().bind(BitbucketGrantAccessPage.class).grantAccess();
        }

        AccountsPageAccountOAuthDialog oAuthDialog = account.regenerate();
        Assert.assertEquals(oAuthNew.key, oAuthDialog.getKey());
        Assert.assertEquals(oAuthNew.secret, oAuthDialog.getSecret());
    }

    /**
     * Tests if new integrated account is added.
     */
    @Test
    public void testNewIntegratedAccount()
    {
        buildOnDemandProperties(new IntegratedAccount(ACCOUNT_NAME, oAuthOriginal.key, oAuthOriginal.secret));
        refreshIntegratedAccounts();

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        Assert.assertTrue("Provided account has to be integrated account/OnDemand account!", account.isOnDemand());
    }

    /**
     * Tests if an existing account is switched into the integrated account.
     */
    @Test
    public void testSwitchToIntegratedAccount()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jira);
        repositoriesPageController.addOrganization(
                it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController.AccountType.BITBUCKET, ACCOUNT_NAME,
                new OAuthCredentials(oAuthOriginal.key, oAuthOriginal.secret), false);

        buildOnDemandProperties(new IntegratedAccount(ACCOUNT_NAME, oAuthNew.key, oAuthNew.secret));
        refreshIntegratedAccounts();

        AccountsPage accountsPage = jira.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        Assert.assertTrue("Provided account has to be integrated account/OnDemand account!", account.isOnDemand());
    }

    /**
     * Removes all integrated accounts.
     */
    private void removeAllIntegratedAccounts()
    {
        buildOnDemandProperties();
        refreshIntegratedAccounts();
        new WebDriverWait(jira.getTester().getDriver(), 30).until(new Predicate<WebDriver>()
        {

            @Override
            public boolean apply(@Nullable WebDriver input)
            {
                AccountsPage accountsPage = jira.visit(AccountsPage.class);
                Iterator<AccountsPageAccount> accounts = accountsPage.getAccounts().iterator();
                while (accounts.hasNext())
                {
                    if (accounts.next().isOnDemand())
                    {
                        return false;
                    }
                }

                return true;
            }

        });
    }

    /**
     * Refreshes integrated accounts.
     */
    private void refreshIntegratedAccounts()
    {
        try
        {
            String restUrl = jira.getProductInstance().getBaseUrl() + "/rest/bitbucket/1.0/integrated-accounts/reload";
            GetMethod getMethod = new GetMethod(restUrl);
            Assert.assertEquals(200, new HttpClient().executeMethod(getMethod));

            try
            {
                Thread.sleep(5000);
            } catch (InterruptedException e)
            {
                throw new RuntimeException(e);

            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);

        }
    }

    /**
     * Builds ondemand.properties for provided accounts.
     * 
     * @param accounts
     *            integrated accounts
     */
    private void buildOnDemandProperties(IntegratedAccount... accounts)
    {
        try
        {
            File onDemandConfigurationFile = new File(onDemandConfigurationPath);

            // creates parent structure
            if (!onDemandConfigurationFile.getParentFile().exists())
            {
                onDemandConfigurationFile.getParentFile().mkdirs();
            }

            if (!onDemandConfigurationFile.exists())
            {
                onDemandConfigurationFile.createNewFile();
            }

            FileWriter onDemandConfigurationWriter = new FileWriter(onDemandConfigurationFile);
            JSONWriter onDemandConfigurationJSON = new JSONWriter(onDemandConfigurationWriter);

            // root
            onDemandConfigurationJSON.object();

            // root/sysadmin-application-links[]
            onDemandConfigurationJSON.key("sysadmin-application-links").array();

            // root/sysadmin-application-links[]/bitbucket[]
            onDemandConfigurationJSON.object();
            onDemandConfigurationJSON.key("bitbucket").array();

            for (IntegratedAccount account : accounts)
            {
                onDemandConfigurationJSON.object();
                onDemandConfigurationJSON.key("account").value(account.name);
                onDemandConfigurationJSON.key("key").value(account.key);
                onDemandConfigurationJSON.key("secret").value(account.secret);
                onDemandConfigurationJSON.endObject();
            }

            // end: root/sysadmin-application-links[]/bitbucket[]
            onDemandConfigurationJSON.endArray();
            onDemandConfigurationJSON.endObject();

            // end: root/sysadmin-application-links[]
            onDemandConfigurationJSON.endArray();

            // end: root
            onDemandConfigurationJSON.endObject();

            // close file
            onDemandConfigurationWriter.close();

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        } catch (JSONException e)
        {
            throw new RuntimeException(e);

        }
    }

}
