package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuth;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketLoginPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.RepositoryRemoteRestpoint;

public class BitbucketRandomRepositoryTestHelper extends RandomRepositoryTestHelper
{
    public enum DvcsType
    {
        GIT, MERCURIAL
    }

    ;
    private final DvcsType dvcsType;

    /**
     * Default constructor uses Git
     */
    public BitbucketRandomRepositoryTestHelper(final String userName, final String password,
            final JiraTestedProduct jiraTestedProduct)
    {
        super(userName, password, jiraTestedProduct);
        dvcsType = DvcsType.GIT;
    }

    /**
     * Constructor that can be used to setup a specific based dvcs
     */
    public BitbucketRandomRepositoryTestHelper(final String userName, final String password,
            final JiraTestedProduct jiraTestedProduct, final DvcsType dvcsType)
    {
        super(userName, password, jiraTestedProduct);
        this.dvcsType = dvcsType;
    }

    @Override
    public void initialiseOrganizationsAndDvcs(final Dvcs dvcs, final OAuth oAuth)
    {
        if (dvcs == null)
        {
            if (dvcsType == DvcsType.MERCURIAL)
            {
                this.dvcs = new MercurialDvcs();
            }
            else
            {
                this.dvcs = new GitDvcs();
            }
        }
        else
        {
            this.dvcs = dvcs;
        }

        if (oAuth == null)
        {
            // only need to login if no OAuth token provided, assume login has been performed if OAuth is provided
            new MagicVisitor(jiraTestedProduct).visit(BitbucketLoginPage.class).doLogin(userName, password);
            this.oAuth = new MagicVisitor(jiraTestedProduct).visit(BitbucketOAuthPage.class, userName).addConsumer();
        }
        else
        {
            this.oAuth = oAuth;
        }

        // adds Bitbucket account into Jira
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.BITBUCKET, userName, new OAuthCredentials(this.oAuth.key, this.oAuth.secret), false);
    }

    @Override
    public void cleanupAccountAndOAuth()
    {
        super.cleanupAccountAndOAuth();
        new MagicVisitor(jiraTestedProduct).visit(BitbucketOAuthPage.class, userName).removeConsumer(oAuth.applicationId);
    }

    @Override
    public void setupTestRepository(String repositoryName)
    {
        BitbucketRemoteClient bbRemoteClient = new BitbucketRemoteClient(userName, password);
        RepositoryRemoteRestpoint repositoryService = bbRemoteClient.getRepositoriesRest();

        BitbucketRepository remoteRepository = repositoryService.createRepository(repositoryName, dvcs.getDvcsType(), false);
        testRepositories.add(remoteRepository);
        dvcs.createTestLocalRepository(userName, repositoryName, userName, password);
    }

    @Override
    public void cleanupLocalRepositories(TimestampNameTestResource timestampNameTestResource)
    {
        // delete account in local configuration first to avoid 404 error when uninstalling hook
        dvcs.deleteAllRepositories();

        BitbucketRemoteClient bbRemoteClient = new BitbucketRemoteClient(userName, password);
        RepositoryRemoteRestpoint repositoryService = bbRemoteClient.getRepositoriesRest();

        for (BitbucketRepository testRepository : testRepositories)
        {
            repositoryService.removeRepository(testRepository.getOwner(), testRepository.getSlug());
        }
        testRepositories.clear();

        removeExpiredRepositories(timestampNameTestResource);
    }

    @Override
    public AccountsPageAccount.AccountType getAccountType()
    {
        return AccountsPageAccount.AccountType.BITBUCKET;
    }
}
