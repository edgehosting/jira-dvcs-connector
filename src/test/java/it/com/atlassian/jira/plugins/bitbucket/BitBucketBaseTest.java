package it.com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.pageobjects.component.BitBucketCommitEntry;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.BaseConfigureRepositoriesPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.GithubOAuthConfigPage;
import com.atlassian.jira.plugins.bitbucket.pageobjects.page.JiraViewIssuePage;
import com.atlassian.pageobjects.TestedProductFactory;
import com.atlassian.webdriver.jira.JiraTestedProduct;
import org.junit.After;
import org.junit.Before;

import java.util.List;

/**
 * Base class for BitBucket integration tests. Initializes the JiraTestedProduct and logs admin in.
 */
public abstract class BitBucketBaseTest
{
    protected static JiraTestedProduct jira = TestedProductFactory.create(JiraTestedProduct.class);
    protected BaseConfigureRepositoriesPage configureRepos;

    @SuppressWarnings("unchecked")
    @Before
    public void loginToJira()
    {
        configureRepos = (BaseConfigureRepositoriesPage) jira.gotoLoginPage().loginAsSysAdmin(getPageClass());
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
        if (!configureRepos.isRepositoryPresent(projectKey, repoUrl))
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

    protected GithubOAuthConfigPage goToGithubOAuthConfigPage()
    {
        return jira.visit(GithubOAuthConfigPage.class);
    }

    protected BaseConfigureRepositoriesPage goToRepositoriesConfigPage()
    {
        configureRepos = (BaseConfigureRepositoriesPage) jira.visit(getPageClass());
        configureRepos.setJiraTestedProduct(jira);
        return configureRepos;
    }
}
