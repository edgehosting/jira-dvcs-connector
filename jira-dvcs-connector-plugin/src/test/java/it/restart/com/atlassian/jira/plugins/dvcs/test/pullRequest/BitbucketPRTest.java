package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.BitbucketPRClient;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.MercurialDvcs;

import java.util.ArrayList;
import java.util.Collection;

public class BitbucketPRTest extends PullRequestTestCases<BitbucketPullRequest>
{
    private Collection<BitbucketRepository> testRepositories = new ArrayList<BitbucketRepository>();

    public BitbucketPRTest()
    {
    }

    @Override
    protected void beforeEachTestInitialisation(final JiraTestedProduct jiraTestedProduct)
    {
        dvcs = new MercurialDvcs();
        pullRequestClient = new BitbucketPRClient();
        addOrganizations(jiraTestedProduct);
    }

    private void addOrganizations(final JiraTestedProduct jiraTestedProduct)
    {
        new MagicVisitor(jiraTestedProduct).visit(BitbucketLoginPage.class).doLogin(ACCOUNT_NAME, PASSWORD);

        // Creates & adds OAuth settings
        oAuth = new MagicVisitor(jiraTestedProduct).visit(BitbucketOAuthPage.class).addConsumer();

        // adds Bitbucket account into Jira
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.BITBUCKET, ACCOUNT_NAME, getOAuthCredentials(), false);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.BITBUCKET, FORK_ACCOUNT_NAME, getOAuthCredentials(), false);
    }

    @Override
    protected void initLocalTestRepository()
    {
        BitbucketRemoteClient bbRemoteClient = new BitbucketRemoteClient(ACCOUNT_NAME, PASSWORD);
        RepositoryRemoteRestpoint repositoryService = bbRemoteClient.getRepositoriesRest();

        BitbucketRepository remoteRepository = repositoryService.createRepository(repositoryName, dvcs.getDvcsType(), false);
        testRepositories.add(remoteRepository);
        dvcs.createTestLocalRepository(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);
    }

    @Override
    protected void cleanupLocalTestRepository()
    {
        BitbucketRemoteClient bbRemoteClient = new BitbucketRemoteClient(ACCOUNT_NAME, PASSWORD);
        RepositoryRemoteRestpoint repositoryService = bbRemoteClient.getRepositoriesRest();

        for (BitbucketRepository testRepository : testRepositories)
        {
            repositoryService.removeRepository(testRepository.getOwner(), testRepository.getSlug());
        }

        dvcs.deleteAllRepositories();

        removeExpiredRepositories(ACCOUNT_NAME, PASSWORD);
        removeExpiredRepositories(FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);
    }

    private void removeExpiredRepositories(String owner, String password)
    {
        BitbucketRemoteClient bbRemoteClient = new BitbucketRemoteClient(owner, password);
        RepositoryRemoteRestpoint repositoryService = bbRemoteClient.getRepositoriesRest();

        for ( BitbucketRepository repository : repositoryService.getAllRepositories(owner))
        {
            if (timestampNameTestResource.isExpired(repository.getName()))
            {
                try
                {
                    repositoryService.removeRepository(repository.getName(), owner);
                }
                catch (BitbucketRequestException.NotFound_404 e) {} // the repo does not exist
            }
        }
    }

    @Override
    protected AccountsPageAccount.AccountType getAccountType()
    {
        return AccountsPageAccount.AccountType.BITBUCKET;
    }
}
