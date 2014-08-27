package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.pageobjects.ProductInstance;
import com.google.common.util.concurrent.Uninterruptibles;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoryDiv;
import org.testng.annotations.Listeners;

import java.util.concurrent.TimeUnit;

import static it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController.AccountType;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Base class of all webdriver tests.
 * <p/>
 * It adds screenshot rule that would capture screenshot when a test fails.
 */
// note that adding this annotations applies the listeners to all tests, but that's exactly what we want
@Listeners ({ WebDriverScreenshotListener.class })
public abstract class DvcsWebDriverTestCase
{
    protected String getJiraCallbackUrlForRepository(OrganizationDiv organisation, ProductInstance productInstance, String repositoryName)
    {
        // Always on the path bitbucket
        String repositoryUrl = productInstance.getBaseUrl() + "/rest/bitbucket/1.0/repository/";

        RepositoryDiv createdOrganisation = organisation.findRepository(repositoryName);
        if (createdOrganisation != null)
        {
            repositoryUrl += createdOrganisation.getRepositoryId() + "/sync";
        }

        return repositoryUrl;
    }

    protected void testPostCommitHookAddedAndRemoved(AccountType accountType, String repositoryName,
            JiraTestedProduct jira, OAuthCredentials oAuthCredentials)
    {
        RepositoriesPageController rpc = new RepositoriesPageController(jira);
        OrganizationDiv organisation = rpc.addOrganization(accountType, ACCOUNT_NAME, oAuthCredentials, true);

        // check postcommit hook is there
        String jiraCallbackUrl = getJiraCallbackUrlForRepository(organisation, jira.getProductInstance(), repositoryName);

        assertTrue(postCommitHookExists(jiraCallbackUrl), "Could not find postcommit hook %s " + jiraCallbackUrl);

        // delete repository
        rpc.getPage().deleteAllOrganizations();

        // check that postcommit hook is removed.
        final long maxWaitTime = 30000;
        final long sleepInterval = 100;
        long totalWaitTime = 0;
        do
        {
            if (!postCommitHookExists(jiraCallbackUrl))
            {
                // test passed
                return;
            }
            Uninterruptibles.sleepUninterruptibly(sleepInterval, TimeUnit.MILLISECONDS);
            totalWaitTime += sleepInterval;
        }
        while (totalWaitTime < maxWaitTime);

        fail(String.format("Postcommit hook not removed after %d ms", totalWaitTime));
    }

    protected boolean postCommitHookExists(final String jiraCallbackUrl)
    {
        throw new UnsupportedOperationException("The default implementation should not be used.");
    }

}
