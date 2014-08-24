package it.com.atlassian.jira.plugins.dvcs;

import com.atlassian.pageobjects.ProductInstance;
import it.restart.com.atlassian.jira.plugins.dvcs.OrganizationDiv;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoryDiv;
import org.testng.annotations.Listeners;

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
}
