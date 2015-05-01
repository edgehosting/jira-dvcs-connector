package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.mock.component.MockComponentWorker;
import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.BitbucketPRClient;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.BitbucketRepositoryTestHelper;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.RepositoryTestHelper;
import it.util.TestAccounts;
import org.mockito.MockitoAnnotations;

import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.PASSWORD;

public class BitbucketPRTest extends PullRequestTestCases<BitbucketPullRequest>
{
    private static final String BB_ACCOUNT_NAME = TestAccounts.FIRST_ACCOUNT;

    private RepositoryTestHelper repositoryTestHelper;
    private RepositoryTestHelper forkRepositoryTestHelper;

    public BitbucketPRTest()
    {
    }

    @Override
    protected void beforeEachTestClassInitialisation(final JiraTestedProduct jiraTestedProduct)
    {
        repositoryTestHelper = new BitbucketRepositoryTestHelper(ACCOUNT_NAME, PASSWORD, getJiraTestedProduct(),
                BitbucketRepositoryTestHelper.DvcsType.MERCURIAL);
        repositoryTestHelper.initialiseOrganizationsAndDvcs(null, null);

        this.dvcs = repositoryTestHelper.getDvcs();
        this.oAuth = repositoryTestHelper.getoAuth();

        forkRepositoryTestHelper = new BitbucketRepositoryTestHelper(FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD,
                getJiraTestedProduct(), BitbucketRepositoryTestHelper.DvcsType.MERCURIAL);
        forkRepositoryTestHelper.initialiseOrganizationsAndDvcs(dvcs, oAuth);

        MockitoAnnotations.initMocks(this);
        final MockComponentWorker mockComponentWorker = new MockComponentWorker().init();
        mockComponentWorker.getMockApplicationProperties().setEncoding("US-ASCII");
        pullRequestClient = new BitbucketPRClient();
    }

    @Override
    protected void cleanupAfterClass()
    {
        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketOAuthPage.class, BB_ACCOUNT_NAME).removeConsumer(oAuth.applicationId);
    }

    @Override
    protected void initLocalTestRepository()
    {
        repositoryTestHelper.setupTestRepository(repositoryName);
    }

    @Override
    protected void cleanupLocalTestRepository()
    {
        repositoryTestHelper.cleanupLocalRepositories(timestampNameTestResource);
    }

    @Override
    protected AccountsPageAccount.AccountType getAccountType()
    {
        return AccountsPageAccount.AccountType.BITBUCKET;
    }
}
