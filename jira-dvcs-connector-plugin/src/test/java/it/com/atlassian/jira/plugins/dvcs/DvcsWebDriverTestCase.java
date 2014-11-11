package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.pageobjects.ProductInstance;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.OrganizationDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.RepositoryDiv;
import org.testng.annotations.Listeners;

import static com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController.AccountType;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static org.fest.assertions.api.Assertions.assertThat;
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
        assertThat(postCommitHookExists(jiraCallbackUrl)).isFalse();
    }

    protected boolean postCommitHookExists(final String jiraCallbackUrl)
    {
        throw new UnsupportedOperationException("The default implementation should not be used.");
    }

}
