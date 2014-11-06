package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.BitbucketOAuthPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.BitbucketPRClient;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.BitbucketRandomRepositoryTestHelper;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.RandomRepositoryTestHelper;

public class BitbucketPRTest extends PullRequestTestCases<BitbucketPullRequest>
{
    private static final String BB_ACCOUNT_NAME = "jirabitbucketconnector";

    private RandomRepositoryTestHelper repositoryTestHelper;
    private RandomRepositoryTestHelper forkRepositoryTestHelper;

    public BitbucketPRTest()
    {
    }

    @Override
    protected void beforeEachTestClassInitialisation(final JiraTestedProduct jiraTestedProduct)
    {
        repositoryTestHelper = new BitbucketRandomRepositoryTestHelper(ACCOUNT_NAME, PASSWORD, getJiraTestedProduct(), false);
        repositoryTestHelper.initialiseOrganizationsAndDvcs(null, null);

        this.dvcs = repositoryTestHelper.getDvcs();
        this.oAuth = repositoryTestHelper.getoAuth();

        forkRepositoryTestHelper = new BitbucketRandomRepositoryTestHelper(FORK_ACCOUNT_NAME, PASSWORD, getJiraTestedProduct(), false);
        forkRepositoryTestHelper.initialiseOrganizationsAndDvcs(dvcs, oAuth);

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
