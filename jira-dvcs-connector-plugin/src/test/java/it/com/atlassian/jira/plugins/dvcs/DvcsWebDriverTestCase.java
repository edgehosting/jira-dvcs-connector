package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.OrganizationDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.component.RepositoryDiv;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.pageobjects.ProductInstance;
import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.google.common.base.Predicate;
import org.testng.annotations.Listeners;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

import static com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController.AccountType;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

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

        assertTrue(retryingCheckPostCommitHooksExists(jiraCallbackUrl, true), "Could not find postcommit hook %s " + jiraCallbackUrl);

        // delete repository
        rpc.getPage().deleteAllOrganizations();

        // check that postcommit hook is removed.
        assertFalse(retryingCheckPostCommitHooksExists(jiraCallbackUrl, false), "Post commit hook not removed");
    }

    /**
     * Retries calls to #postCommitHookExists retrying for 10 seconds or until the returned value matches #expectedValue
     *
     * @param jiraCallbackUrl Url to pass to the #postCommitHookExists call
     * @param expectedValue The expected value, retries until the result from #postCommitHookExists matches this value
     * OR the timeout of 10 seconds is reached
     * @return The result of calling #postCommitHookExists or ! #expectedValue if the retry limit is exceeded
     */
    private boolean retryingCheckPostCommitHooksExists(final String jiraCallbackUrl, final boolean expectedValue)
    {
        Callable<Boolean> doesPostCommitHookExist = new Callable<Boolean>()
        {
            public Boolean call() throws Exception
            {
                return postCommitHookExists(jiraCallbackUrl);
            }
        };

        Predicate<Boolean> retryPredicate = new Predicate<Boolean>()
        {
            public boolean apply(final Boolean input)
            {
                return input != expectedValue;
            }
        };

        RetryerBuilder<Boolean> retryerBuilder = RetryerBuilder.<Boolean>newBuilder()
                .retryIfResult(retryPredicate)
                .withStopStrategy(StopStrategies.stopAfterDelay(10000));

        final Retryer<Boolean> retryer = retryerBuilder.build();

        try
        {
            return retryer.call(doesPostCommitHookExist);
        }
        catch (ExecutionException e)
        {
            throw new RuntimeException(e);
        }
        catch (RetryException e)
        {
            // We didn't find the value we wanted after retrying, one last call and we will use that result
            return postCommitHookExists(jiraCallbackUrl);
        }
    }

    protected boolean postCommitHookExists(final String jiraCallbackUrl)
    {
        throw new UnsupportedOperationException("The default implementation should not be used.");
    }

}
