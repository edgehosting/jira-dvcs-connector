package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.plugins.dvcs.base.resource.TimestampNameTestResource;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPullRequest;
import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Base class that contains the test cases for the PullRequest scenarios
 */
public abstract class PullRequestTestCases extends AbstractDVCSTest
{
    protected static final int EXPIRATION_DURATION_5_MIN = 5 * 60 * 1000;
    protected static final String TEST_PROJECT_KEY = "TST";

    protected static final String COMMIT_AUTHOR = "Stanislav Dvorscak";
    private static final String COMMIT_AUTHOR_EMAIL = "sdvorscak@atlassian.com";

    /**
     * Repository owner.
     */
    protected static final String ACCOUNT_NAME = "jirabitbucketconnector";

    /**
     * Fork repository owner.
     */
    protected static final String FORK_ACCOUNT_NAME = "dvcsconnectortest";

    /**
     * Appropriate {@link #ACCOUNT_NAME} password.
     */
    protected static final String PASSWORD = System.getProperty("jirabitbucketconnector.password");

    /**
     * Appropriate {@link #FORK_ACCOUNT_NAME} password.
     */
    protected static final String FORK_ACCOUNT_PASSWORD = System.getProperty("dvcsconnectortest.password");

    protected final Dvcs dvcs;

    protected TimestampNameTestResource timestampNameTestResource = new TimestampNameTestResource();

    /**
     * Issue key used for testing.
     */
    protected String issueKey;

    protected String repositoryName;

    public PullRequestTestCases(Dvcs dvcs)
    {
        this.dvcs = dvcs;
    }

    /**
     * Open a Pull Request to the appropriate provider and return the URL for the Pull RequestØ
     */
    protected abstract String openPullRequest(String owner, String repositoryName, String password, String title, String description, String head, String base, String... reviewers);

    protected abstract String getTestIssueSummary();

    protected abstract String getRepositoryNameSuffix();

    /**
     * Setup the Organisation links in JIRA
     */
    protected abstract void addOrganizations();

    @BeforeClass
    public void beforeEachPullRequestTestClass()
    {
        new JiraLoginPageController(getJiraTestedProduct()).login();
        addOrganizations();
    }

    @AfterClass (alwaysRun = true)
    public void afterEachPullRequestTestClass()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(getJiraTestedProduct());
        repositoriesPageController.getPage().deleteAllOrganizations();
    }

    @BeforeMethod
    protected void beforeEachPullRequestTest()
    {
        repositoryName = timestampNameTestResource.randomName(getRepositoryNameSuffix(), EXPIRATION_DURATION_5_MIN);
        initLocalTestRepository();
        issueKey = addTestIssue(TEST_PROJECT_KEY, getTestIssueSummary());
    }

    protected abstract void initLocalTestRepository();

    @AfterMethod
    protected void afterEachPullRequestTest(){
        cleanupLocalTestRepository();
    }

    protected abstract void cleanupLocalTestRepository();

    protected AccountsPageAccount refreshAccount(final String accountName)
    {
        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountsPageAccount.AccountType.BITBUCKET, accountName);
        account.refresh();

        return account;
    }

    protected void sleep(final long millis)
    {
        try
        {
            Thread.sleep(millis);
        }
        catch (InterruptedException e)
        {
        }
    }

    @Test (groups = { "PRTestCases" })
    public void testOpenPullRequestBranch()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        String[] commitNodeOpen = new String[2];

        dvcs.addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        dvcs.commit(ACCOUNT_NAME, repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        dvcs.push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        dvcs.createBranch(ACCOUNT_NAME, repositoryName, fixBranchName);

        dvcs.addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = dvcs.commit(ACCOUNT_NAME, repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        dvcs.addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = dvcs.commit(ACCOUNT_NAME, repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        dvcs.push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        String pullRequestLocation = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestName, "Open PR description", fixBranchName, dvcs.getDefaultBranchName());

        // Wait for remote system after creation of pullRequest
        sleep(1000);

        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        account.synchronizeRepository(repositoryName);

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.OPEN.toString());
        Assert.assertTrue(pullRequestLocation.startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), dvcs.getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
    }
}
