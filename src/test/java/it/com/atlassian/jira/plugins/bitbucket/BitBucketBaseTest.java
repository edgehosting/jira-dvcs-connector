package it.com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.BitBucketConfigureRepositoriesPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.JiraViewIssuePage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import org.junit.After;
import org.junit.Before;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;

/**
 * Base class for BitBucket integration tests. Initializes the JiraTestedProduct and logs admin in.
 */
public abstract class BitBucketBaseTest
{
    protected JiraTestedProduct jira;
    protected BitBucketConfigureRepositoriesPage configureRepos;

    @Before
    public void loginToJira()
    {
        jira = TestedProductFactory.create(JiraTestedProduct.class);

        configureRepos = jira.gotoLoginPage().loginAsSysAdmin(BitBucketConfigureRepositoriesPage.class);
    }

    @After
    public void logout()
    {
        jira.getTester().getDriver().manage().deleteAllCookies();
    }

    protected void ensureRepositoryPresent(String projectKey, String repoUrl)
    {
        if(configureRepos.isRepositoryPresent(projectKey, repoUrl + "/default") == false)
        {
            configureRepos.addPublicRepoToProject(projectKey, repoUrl);
        }
    }

   
    protected List<BitBucketCommitEntry> getCommitsForIssue(String issueKey)
    {
        return jira.visit(JiraViewIssuePage.class, issueKey)
                              .openBitBucketPanel()
                              .waitForMessages();
    }

}
