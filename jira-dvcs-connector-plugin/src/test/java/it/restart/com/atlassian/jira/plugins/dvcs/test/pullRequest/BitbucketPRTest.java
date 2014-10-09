package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BitbucketRequestException;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.BitbucketPRClient;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.MercurialDvcs;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collection;

public class BitbucketPRTest extends PullRequestTestCases<BitbucketPullRequest>
{
    private static final String BB_ACCOUNT_NAME = "jirabitbucketconnector";
    private Collection<BitbucketRepository> testRepositories = new ArrayList<BitbucketRepository>();

    public BitbucketPRTest()
    {
    }

    @Override
    protected void beforeEachTestClassInitialisation(final JiraTestedProduct jiraTestedProduct)
    {
        dvcs = new MercurialDvcs();
        // need to initialize ComponentWorker for UrlBuilder in PullRequestRemoteRestpoint used by BitbucketClient
        //  set the encoding from ApplicationProperties, which is required by UrlBuilder.addPath()
        MockitoAnnotations.initMocks(this);
        final MockComponentWorker mockComponentWorker = new MockComponentWorker().init();
        mockComponentWorker.getMockApplicationProperties().setEncoding("US-ASCII");

        pullRequestClient = new BitbucketPRClient();
        addOrganizations(jiraTestedProduct);
    }

    @Override
    protected void cleanupAfterClass()
    {
        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).removeConsumer(oAuth.applicationId);
    }

    private void addOrganizations(final JiraTestedProduct jiraTestedProduct)
    {
        new MagicVisitor(jiraTestedProduct).visit(BitbucketLoginPage.class).doLogin(ACCOUNT_NAME, PASSWORD);

        // Creates & adds OAuth settings
        oAuth = new MagicVisitor(jiraTestedProduct).visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).addConsumer();

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
                catch (BitbucketRequestException.NotFound_404 ignored) {} // the repo does not exist
            }
        }
    }

    @Override
    protected AccountsPageAccount.AccountType getAccountType()
    {
        return AccountsPageAccount.AccountType.BITBUCKET;
    }
}
