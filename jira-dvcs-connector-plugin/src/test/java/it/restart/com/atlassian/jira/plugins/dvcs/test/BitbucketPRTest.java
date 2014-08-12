package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.BitbucketPRClient;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.MercurialDvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.PullRequestClient;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;

import java.util.ArrayList;
import java.util.Collection;

public class BitbucketPRTest extends PullRequestTestCases
{
    private Collection<BitbucketRepository> testRepositories = new ArrayList<BitbucketRepository>();

    @Factory (dataProvider = "dvcs")
    public BitbucketPRTest(final Dvcs dvcs, final PullRequestClient pullRequestClient)
    {
        super(dvcs, pullRequestClient);
    }

    /**
     * Git appears broken for the moment, commented out
     */
    @DataProvider
    public static Object[][] dvcs()
    {
        return new Object[][]
                {
                        new Object[] { new MercurialDvcs(), new BitbucketPRClient() }
//                        new Object[] { new GitDvcs() }
                };
    }

    @Override
    protected String getTestIssueSummary()
    {
        return BitbucketPRTest.class.getCanonicalName();
    }

    @Override
    protected String getRepositoryNameSuffix()
    {
        return BitbucketPRTest.class.getSimpleName().toLowerCase();
    }

    @Override
    protected void addOrganizations()
    {
        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketLoginPage.class).doLogin(ACCOUNT_NAME, PASSWORD);

        // Creates & adds OAuth settings
        oAuth = new MagicVisitor(getJiraTestedProduct()).visit(BitbucketOAuthPage.class).addConsumer();

        // adds Bitbucket account into Jira
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(getJiraTestedProduct());
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
    }

    private String getUriKey(String owner, String slug)
    {
        return owner + "/" + slug;
    }
}
