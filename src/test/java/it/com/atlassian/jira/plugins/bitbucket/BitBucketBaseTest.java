package it.com.atlassian.jira.plugins.bitbucket;

import java.util.List;

import org.junit.After;
import org.junit.Before;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.BaseConfigureRepositoriesPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.JiraViewIssuePage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.jira.JiraTestedProduct;

/**
 * Base class for BitBucket integration tests. Initializes the JiraTestedProduct and logs admin in.
 */
public abstract class BitBucketBaseTest
{
    protected JiraTestedProduct jira;
    protected BaseConfigureRepositoriesPage configureRepos;

    @SuppressWarnings("unchecked")
    @Before
    public void loginToJira()
    {
        jira = TestedProductFactory.create(JiraTestedProduct.class);

        configureRepos = (BaseConfigureRepositoriesPage) jira.gotoLoginPage().loginAsSysAdmin(getPageClass());
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
