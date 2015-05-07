package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPullRequest;
import com.atlassian.jira.plugins.dvcs.pageobjects.JiraLoginPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.RepositoriesPageController;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPage;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.PullRequestLocalRestpoint;
import it.restart.com.atlassian.jira.plugins.dvcs.test.AbstractDVCSTest;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.PullRequestClient;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Collection;

import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.ACCOUNT_NAME;
import static it.restart.com.atlassian.jira.plugins.dvcs.test.IntegrationTestUserDetails.PASSWORD;

/**
 * Base class that contains the test cases for the PullRequest scenarios.
 * <p/>
 * Implementors will need to override the abstract methods as required. If necessary you may need to provide a custom
 * implementation of {@link it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs} or {@link
 * it.restart.com.atlassian.jira.plugins.dvcs.testClient.PullRequestClient}.
 * <p/>
 * Also due to the coupling between this class and its implementors you should be wary of relying on the state of parent
 * member variables like {@link #dvcs} or {@link #getJiraTestedProduct()} which should be intialised during {@link
 * #beforeEachPullRequestTest()}.
 */
public abstract class PullRequestTestCases<T> extends AbstractDVCSTest
{
    protected static final int EXPIRATION_DURATION_5_MIN = 5 * 60 * 1000;
    protected static final String TEST_PROJECT_KEY = "TST";

    protected static final String COMMIT_AUTHOR = "Jira DvcsConnector";
    private static final String COMMIT_AUTHOR_EMAIL = "jirabitbucketconnector@atlassian.com"; // fake email

    private static final String TEST_DATA = "test-dvcs.zip";

    /**
     * Fork repository owner.
     */
    protected static final String FORK_ACCOUNT_NAME = "dvcsconnectortest";

    /**
     * Appropriate {@link #FORK_ACCOUNT_NAME} password.
     */
    protected static final String FORK_ACCOUNT_PASSWORD = System.getProperty("dvcsconnectortest.password");

    protected Dvcs dvcs;
    protected PullRequestClient<T> pullRequestClient;

    protected TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();
    protected DvcsPRTestHelper dvcsPRTestHelper;

    /**
     * Issue key used for testing.
     */
    protected String issueKey;

    protected String repositoryName;

    public PullRequestTestCases()
    {
    }

    protected String getTestIssueSummary()
    {
        return this.getClass().getCanonicalName();
    }

    protected String getRepositoryNameSuffix()
    {
        return this.getClass().getSimpleName().toLowerCase();
    }

    @BeforeClass
    public void beforeEachPullRequestTestClass()
    {
        getJiraTestedProduct().backdoor().restoreDataFromResource(TEST_DATA);
        new JiraLoginPageController(getJiraTestedProduct()).login();

        beforeEachTestClassInitialisation(getJiraTestedProduct());

        dvcsPRTestHelper = new DvcsPRTestHelper(dvcs, ACCOUNT_NAME, COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL, PASSWORD);
    }

    /**
     * Setup prior to running a test case, implementing classes <b>MUST</b> associate the organisations in JIRA
     * <b>AND</b> initialise the following member variables on the parent: <ul> <li>PullRequestTestCases#dvcs</li>
     * <li>PullRequestTestCases#pullRequestClient</li> </ul>
     */
    protected abstract void beforeEachTestClassInitialisation(JiraTestedProduct jiraTestedProduct);

    @AfterClass (alwaysRun = true)
    public void afterEachPullRequestTestClass()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(getJiraTestedProduct());
        repositoriesPageController.getPage().deleteAllOrganizations();
        cleanupAfterClass();
    }

    /**
     * Note that we are moving some of the common code from this into #RepositoryTestHelper, Github and GHE are not
     * done at this stage.
     */
    protected abstract void cleanupAfterClass();

    @BeforeMethod
    protected void beforeEachPullRequestTest()
    {
        repositoryName = timestampNameTestResource.randomName(getRepositoryNameSuffix(), EXPIRATION_DURATION_5_MIN);
        initLocalTestRepository();
        issueKey = addTestIssue(TEST_PROJECT_KEY, getTestIssueSummary());
    }

    /**
     * Initialise the local repository for testing, called before each method
     *
     * @see #beforeEachPullRequestTest()
     */
    protected abstract void initLocalTestRepository();

    @AfterMethod
    protected void afterEachPullRequestTest()
    {
        cleanupLocalTestRepository();
    }

    /**
     * Called after each test method, clean up any test repositories
     *
     * @see #afterEachPullRequestTest()
     */
    protected abstract void cleanupLocalTestRepository();

    /**
     * The type of account we should look for when refreshing
     */
    protected abstract AccountsPageAccount.AccountType getAccountType();

    @Test
    public void testOpenPullRequestUpdateApproveAndMerge()
    {
        // skipping this test for GHE as the test is very flakey, see BBC-895
        if (getAccountType() == AccountsPageAccount.AccountType.GIT_HUB_ENTERPRISE)
        {
            return;
        }
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        Collection<String> firstRoundCommits = dvcsPRTestHelper.createBranchAndCommits(repositoryName, fixBranchName, issueKey, 2);

        PullRequestClient.PullRequestDetails<T> pullRequestDetails = pullRequestClient.openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestName, "Open PR description",
                fixBranchName, dvcs.getDefaultBranchName());
        String pullRequestLocation = pullRequestDetails.getLocation();

        RestPrRepository restPrRepository = refreshSyncAndGetFirstPrRepository();

        RestPrRepositoryPRTestAsserter asserter = new RestPrRepositoryPRTestAsserter(repositoryName, pullRequestLocation, pullRequestName, ACCOUNT_NAME,
                fixBranchName, dvcs.getDefaultBranchName());

        asserter.assertBasicPullRequestConfiguration(restPrRepository, firstRoundCommits);

        // We will check up on these later once there has been a status change and a full sync of the PR
        Collection<String> secondRoundCommits = dvcsPRTestHelper.addMoreCommitsAndPush(repositoryName, fixBranchName, issueKey, 2);

        final String updatedPullRequestName = pullRequestName + "updated";
        pullRequestDetails = pullRequestClient.updatePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestDetails.getPullRequest(),
                updatedPullRequestName, "updated desc", dvcs.getDefaultBranchName());

        if (getAccountType() != AccountsPageAccount.AccountType.BITBUCKET)
        {
            // skip adding PR comment for BB until we have page objects to do so (BB Rest Api does not support that atm)
            pullRequestClient.commentPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestDetails.getPullRequest(), "Some comment after update");
        }

        if (getAccountType() == AccountsPageAccount.AccountType.BITBUCKET)
        {
            pullRequestClient.approvePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestDetails.getId());

            sleep(500);

            restPrRepository = refreshSyncAndGetFirstPrRepository();
            asserter.assertPullRequestApproved(restPrRepository.getPullRequests().get(0));
        }

        pullRequestClient.mergePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestDetails.getId());

        sleep(1000);

        restPrRepository = refreshSyncAndGetFirstPrRepository();

        final RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.MERGED.toString());
        Assert.assertEquals(restPullRequest.getTitle(), updatedPullRequestName);

        // Comments are not critical so let us just check them at the end rather than having a separate refresh and wait
        Assert.assertEquals(restPullRequest.getCommentCount(), getAccountType() != AccountsPageAccount.AccountType.BITBUCKET ? 1 : 0);

        // Commits should be pulled in now that we have merged
        Collection<String> allCommits = new ArrayList<String>(firstRoundCommits);
        allCommits.addAll(secondRoundCommits);

        asserter.assertCommitsMatch(restPrRepository.getPullRequests().get(0), allCommits);
    }

    private RestPrRepository refreshSyncAndGetFirstPrRepository()
    {
        AccountsPageAccount account = AccountsPage.syncAccount(getJiraTestedProduct(), getAccountType(),
                ACCOUNT_NAME, repositoryName, true);

        PullRequestLocalRestpoint prRest = new PullRequestLocalRestpoint();

        // Event processing can take some time to complete, poll the endpoint to find our PR
        RestDevResponse<RestPrRepository> response = prRest.getAtLeastOnePullRequest(issueKey);
        Assert.assertEquals(response.getRepositories().size(), 1);
        return response.getRepositories().get(0);
    }

    protected void sleep(final long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException ignored)
        {
        }
    }
}
