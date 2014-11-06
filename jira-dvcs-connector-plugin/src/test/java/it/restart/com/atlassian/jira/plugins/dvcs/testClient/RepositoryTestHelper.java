package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A helper class to work with a randomly named repository, holds some relevant state to allow for cleanup in tests.
 *
 * Concrete implementations of this class will be specialised to a particular remote provider (Bitbucket, Github,
 * Github Enterprise) and Dvcs type (Git, Mercurial). The methods are expected to be used as follows:
 * <ul>
 *     <li>#initialiseOrganizationsAndDvcs will be called during the once per class initialisation</li>
 *     <li>#cleanupAccountAndOAuth will be called during the once per class teardown</li>
 *     <li>#setupTestRepository will be called per test method to create a repository</li>
 *     <li>#cleanupLocalRepositories will be called per test method to remove created repositories</li>
 * </ul>
 */
public abstract class RepositoryTestHelper
{
    protected final Collection<BitbucketRepository> testRepositories = new ArrayList<BitbucketRepository>();
    protected Dvcs dvcs;
    protected OAuth oAuth;
    protected final String userName;
    protected final String password;
    protected final JiraTestedProduct jiraTestedProduct;

    protected RepositoryTestHelper(final String userName, final String password,
            final JiraTestedProduct jiraTestedProduct)
    {
        this.userName = userName;
        this.password = password;
        this.jiraTestedProduct = jiraTestedProduct;
    }

    /**
     * Create a DVCS Organization and dvcs instance to use
     * @param dvcs The dvcs to use, if not provided then it will be instantiated, see #getDvcs
     * @param oauth The oAuth token, if not provided then it will be created for the relevant provider, see #getoAuth
     */
    public abstract void initialiseOrganizationsAndDvcs(final Dvcs dvcs, final OAuth oauth);

    /**
     * Remove the OAuth token from the provider and any other cleanup, should also remove the Dvcs Organization
     */
    public void cleanupAccountAndOAuth()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.getPage().deleteAllOrganizations();
    }

    /**
     * Create a repository with the given name, usually this will be from a
     * @see {@link com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource}
     * @param repositoryName
     */
    public abstract void setupTestRepository(String repositoryName);

    /**
     * Cleanup any created repositories, will also attempt to remove repositories that are 'expired' as judged by the
     * #timestampNameTestResource
     * @param timestampNameTestResource
     */
    public abstract void cleanupLocalRepositories(TimestampNameTestResource timestampNameTestResource);

    protected void removeExpiredRepositories(TimestampNameTestResource timestampNameTestResource)
    {
        BitbucketRemoteClient bbRemoteClient = new BitbucketRemoteClient(userName, password);
        RepositoryRemoteRestpoint repositoryService = bbRemoteClient.getRepositoriesRest();

        for (BitbucketRepository repository : repositoryService.getAllRepositories(userName))
        {
            if (timestampNameTestResource.isExpired(repository.getName()))
            {
                try
                {
                    repositoryService.removeRepository(repository.getName(), userName);
                }
                catch (BitbucketRequestException.NotFound_404 ignored) {} // the repo does not exist
            }
        }
    }

    public abstract AccountsPageAccount.AccountType getAccountType();

    public Dvcs getDvcs()
    {
        return dvcs;
    }

    public OAuth getoAuth()
    {
        return oAuth;
    }
}
