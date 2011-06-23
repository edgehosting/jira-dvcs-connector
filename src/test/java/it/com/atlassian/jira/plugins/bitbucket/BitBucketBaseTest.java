package it.com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.BitBucketConfigureRepositoriesPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.JiraViewIssuePage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.pageobjects.page.HomePage;
import com.atlassian.webdriver.jira.JiraTestedProduct;
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
