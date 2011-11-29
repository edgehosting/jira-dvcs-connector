package it.com.atlassian.jira.plugins.bitbucket;

import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.By;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.BaseConfigureRepositoriesPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.JiraViewIssuePage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.LoginPage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import com.atlassian.webdriver.jira.page.JiraLoginPage;

/**
 * Base class for BitBucket integration tests. Initializes the JiraTestedProduct and logs admin in.
 */
public abstract class BitBucketBaseTest
{
    protected JiraTestedProduct jira;
    protected BaseConfigureRepositoriesPage configureRepos;

  
    public static class AnotherLoginPage extends JiraLoginPage
    {
        @Override
        public void doWait()
        {
            driver.waitUntilElementIsLocated(By.name("os_username"));
        }
    }
    
    @SuppressWarnings("unchecked")
    @Before
    public void loginToJira()
    {
        jira = TestedProductFactory.create(JiraTestedProduct.class);

        jira.getPageBinder().override(LoginPage.class, AnotherLoginPage.class);
        
        configureRepos = (BaseConfigureRepositoriesPage) jira.getPageBinder().navigateToAndBind(AnotherLoginPage.class).loginAsSysAdmin(getPageClass());
        configureRepos.setJiraTestedProduct(jira);
    }

    @SuppressWarnings("rawtypes")
    protected abstract Class getPageClass();

    @After
    public void logout()
    {
        jira.getTester().getDriver().manage().deleteAllCookies();
    }

    protected void ensureRepositoryPresent(String projectKey, String repoUrl)
    {
        if(configureRepos.isRepositoryPresent(projectKey, repoUrl) == false)
        {
            configureRepos.addPublicRepoToProjectSuccessfully(projectKey, repoUrl);
        }
    }

   
    protected List<BitBucketCommitEntry> getCommitsForIssue(String issueKey)
    {
        return jira.visit(JiraViewIssuePage.class, issueKey)
                              .openBitBucketPanel()
                              .waitForMessages();
    }

}
