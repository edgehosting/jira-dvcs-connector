package it.com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.pageobjects.page.BitBucketConfigureRepositoriesPage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import org.junit.Before;

/**
 * Base class for BitBucket integration tests. Initializes the JiraTestedProduct and logs admin in.
 */
public class BitBucketBaseTest
{
    protected JiraTestedProduct jira;
    protected BitBucketConfigureRepositoriesPage configureRepos;

    @Before
    public void loginToJira()
    {
        System.setProperty("baseurl.jira", "http://localhost:2990/jira");
        System.setProperty("http.jira.port", "2990");
        System.setProperty("context.jira.path", "jira");

        jira = TestedProductFactory.create(JiraTestedProduct.class);

        if(jira.visit(HomePage.class).getHeader().isLoggedIn())
        {
            configureRepos = jira.visit(BitBucketConfigureRepositoriesPage.class);
        }
        else
        {
            configureRepos = jira.gotoLoginPage().loginAsSysAdmin(BitBucketConfigureRepositoriesPage.class);
        }
    }
}
