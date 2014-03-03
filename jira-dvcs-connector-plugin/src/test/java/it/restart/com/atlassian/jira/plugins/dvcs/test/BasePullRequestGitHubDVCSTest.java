package it.restart.com.atlassian.jira.plugins.dvcs.test;

import it.restart.com.atlassian.jira.plugins.dvcs.JiraLoginPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount.AccountType;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository;

import org.eclipse.egit.github.core.PullRequest;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.BaseDVCSTest;
import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestResource;
import com.atlassian.jira.plugins.dvcs.base.resource.GitTestResource;
import com.atlassian.jira.plugins.dvcs.base.resource.JiraTestResource;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPullRequest;
import com.atlassian.jira.plugins.dvcs.model.dev.RestRef;
import com.atlassian.jira.plugins.dvcs.model.dev.RestUser;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.PullRequestLocalRestpoint;
import com.atlassian.pageobjects.TestedProductFactory;

/**
 * Pull request GitHub related tests.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public abstract class BasePullRequestGitHubDVCSTest extends BaseDVCSTest
{

    /**
     * All temporary staff will be discarded when it is alive more than five minutes.
     */
    private static final int EXPIRATION_DURATION_5_MIN = 5 * 60 * 1000;

    /**
     * Key of testing project.
     */
    private static final String TEST_PROJECT_KEY = "TST";

    /**
     * Summary of test issue.
     */
    private static final String TEST_ISSUE_SUMMARY = BasePullRequestGitHubDVCSTest.class.getCanonicalName();

    /**
     * Name of author which will be used as committer.
     */
    private static final String COMMIT_AUTHOR = "Stanislav Dvorscak";

    /**
     * Email of author which will be used as committer.
     */
    private static final String COMMIT_AUTHOR_EMAIL = "sdvorscak@atlassian.com";

    private static final String PR_AUTHOR_NAME = "Janko Hrasko";
    private static final String PR_AUTHOR_USERNAME = "jirabitbucketconnector";
    private static final String PR_AUTHOR_EMAIL = "miroslav.stencel@hotovo.org";

    /**
     * @see JiraTestedProduct
     */
    private JiraTestedProduct jiraTestedProduct = TestedProductFactory.create(JiraTestedProduct.class);

    /**
     * @see GitHubTestResource
     */
    private GitHubTestResource gitHubResource = new GitHubTestResource(this, new MagicVisitor(jiraTestedProduct));

    /**
     * @see GitTestResource
     */
    private GitTestResource gitResource = new GitTestResource(this);

    /**
     * @see JiraTestResource
     */
    private JiraTestResource jiraTestResource = new JiraTestResource(this);

    /**
     * @see PullRequestLocalRestpoint
     */
    private PullRequestLocalRestpoint pullRequestLocalRestpoint = new PullRequestLocalRestpoint();

    /**
     * Name of repository used by tests of this class.
     */
    private String repositoryName;

    /**
     * Issue key used for testing.
     */
    private String issueKey;

    /**
     * Prepares tests environment.
     */
    @BeforeClass
    public void onTestsSetup()
    {
        setupGitHubResource(gitHubResource);
        new JiraLoginPageController(jiraTestedProduct).login();
        addOrganizations();
    }

    /**
     * Adds necessary organization.
     */
    protected abstract void addOrganizations();

    /**
     * Setups {@link GitHubTestResource} - e.g. username/password, URL.
     * 
     * @param gitHubTestResource
     */
    protected abstract void setupGitHubResource(GitHubTestResource gitHubTestResource);
    
    /**
     * @return Type of GitHub account, which is tested.
     */
    protected abstract AccountType getAccountType();

    /**
     * Cleans test environment.
     */
    @AfterClass(alwaysRun = true)
    public void onTestsCleanup()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.getPage().deleteAllOrganizations();
    }

    /**
     * Prepares test environment.
     */
    @BeforeMethod
    public void onTestSetup()
    {

        repositoryName = gitHubResource.addRepository(GitHubTestResource.USER, BasePullRequestGitHubDVCSTest.class.getCanonicalName(),
                GitHubTestResource.Lifetime.DURING_TEST_METHOD, EXPIRATION_DURATION_5_MIN);
        gitResource.addRepository(repositoryName);

        issueKey = jiraTestResource.addIssue(TEST_PROJECT_KEY, TEST_ISSUE_SUMMARY, JiraTestResource.Lifetime.DURING_TEST_METHOD,
                EXPIRATION_DURATION_5_MIN);

    }

    /**
     * Test that "Open Pull Request" is working.
     */
    @Test
    public void testOpenBranch()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        String[] commitNodeOpen = new String[2];

        gitResource.init(repositoryName, gitHubResource.getRepository(GitHubTestResource.USER, repositoryName).getCloneUrl());
        gitResource.addFile(repositoryName, "README.txt", "Hello World!".getBytes());
        gitResource.commit(repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        gitResource.push(repositoryName, GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD);

        gitResource.createBranch(repositoryName, fixBranchName);
        gitResource.checkout(repositoryName, fixBranchName);

        gitResource.addFile(repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = gitResource.commit(repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        gitResource.addFile(repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = gitResource.commit(repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        gitResource.push(repositoryName, GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD, fixBranchName);

        PullRequest pullRequest = gitHubResource.openPullRequest(GitHubTestResource.USER, repositoryName, pullRequestName,
                "Open PR description", fixBranchName, "master");

        AccountsPage accountsPage = jiraTestedProduct.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(getAccountType(), GitHubTestResource.USER);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(repositoryName);
        repository.enable();
        repository.synchronize();

        RestDevResponse<RestPrRepository> pullRequestActual = pullRequestLocalRestpoint.getPullRequest(issueKey);
        Assert.assertEquals(pullRequestActual.getRepositories().size(), 1);
        Assert.assertEquals(pullRequestActual.getRepositories().get(0).getPullRequests().size(), 1);
        RestPullRequest actualPullRequest = pullRequestActual.getRepositories().get(0).getPullRequests().get(0);

        assertPullRequestInfo(actualPullRequest, "OPEN", pullRequestName, pullRequest.getHtmlUrl());

        assertRestRef(actualPullRequest.getSource(), GitHubTestResource.USER, repositoryName, fixBranchName);
        assertRestRef(actualPullRequest.getDestination(), GitHubTestResource.USER, repositoryName, "master");

        Assert.assertEquals(actualPullRequest.getCommentCount(), 0);

        Assert.assertEquals(actualPullRequest.getParticipants().size(), 1);
        assertRestUser(actualPullRequest.getParticipants().get(0).getUser(), PR_AUTHOR_USERNAME, PR_AUTHOR_NAME, PR_AUTHOR_EMAIL);
    }

    /**
     * Test that "Open Pull Request" is working.
     */
    @Test
    public void testComment()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        String[] commitNodeOpen = new String[2];

        gitResource.init(repositoryName, gitHubResource.getRepository(GitHubTestResource.USER, repositoryName).getCloneUrl());
        gitResource.addFile(repositoryName, "README.txt", "Hello World!".getBytes());
        gitResource.commit(repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        gitResource.push(repositoryName, GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD);

        gitResource.createBranch(repositoryName, fixBranchName);
        gitResource.checkout(repositoryName, fixBranchName);

        gitResource.addFile(repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = gitResource.commit(repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        gitResource.addFile(repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = gitResource.commit(repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        gitResource.push(repositoryName, GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD, fixBranchName);

        PullRequest pullRequest = gitHubResource.openPullRequest(GitHubTestResource.USER, repositoryName, pullRequestName,
                "Open PR description", fixBranchName, "master");

        // wait until pull request will be established
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            // nothing to do
        }

        gitHubResource.commentPullRequest(GitHubTestResource.USER, repositoryName, pullRequest, issueKey + ": General Pull Request Comment");

        AccountsPage accountsPage = jiraTestedProduct.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(getAccountType(), GitHubTestResource.USER);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(repositoryName);
        repository.enable();
        repository.synchronize();

        RestDevResponse<RestPrRepository> pullRequestActual = pullRequestLocalRestpoint.getPullRequest(issueKey);
        Assert.assertEquals(pullRequestActual.getRepositories().size(), 1);
        Assert.assertEquals(pullRequestActual.getRepositories().get(0).getPullRequests().size(), 1);
        RestPullRequest actualPullRequest = pullRequestActual.getRepositories().get(0).getPullRequests().get(0);

        assertPullRequestInfo(actualPullRequest, "OPEN", pullRequest.getTitle(), pullRequest.getHtmlUrl());
        Assert.assertEquals(actualPullRequest.getCommentCount(), 1);
    }

    /**
     * Tests that "Decline Pull Request" is working.
     */
    @Test
    public void testDecline()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        String[] commitNodeOpen = new String[2];

        gitResource.init(repositoryName, gitHubResource.getRepository(GitHubTestResource.USER, repositoryName).getCloneUrl());
        gitResource.addFile(repositoryName, "README.txt", "Hello World!".getBytes());
        gitResource.commit(repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        gitResource.push(repositoryName, GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD);

        gitResource.createBranch(repositoryName, fixBranchName);
        gitResource.checkout(repositoryName, fixBranchName);

        gitResource.addFile(repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = gitResource.commit(repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        gitResource.addFile(repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = gitResource.commit(repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        gitResource.push(repositoryName, GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD, fixBranchName);

        PullRequest pullRequest = gitHubResource.openPullRequest(GitHubTestResource.USER, repositoryName, pullRequestName,
                "Open PR description", fixBranchName, "master");

        // gives such time for pull request creation
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            // nothing to do
        }

        gitHubResource.closePullRequest(GitHubTestResource.USER, repositoryName, pullRequest);

        AccountsPage accountsPage = jiraTestedProduct.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(getAccountType(), GitHubTestResource.USER);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(repositoryName);
        repository.enable();
        repository.synchronize();

        RestDevResponse<RestPrRepository> pullRequestActual = pullRequestLocalRestpoint.getPullRequest(issueKey);
        Assert.assertEquals(pullRequestActual.getRepositories().size(), 1);
        Assert.assertEquals(pullRequestActual.getRepositories().get(0).getPullRequests().size(), 1);
        RestPullRequest actualPullRequest = pullRequestActual.getRepositories().get(0).getPullRequests().get(0);

        assertPullRequestInfo(actualPullRequest, "DECLINED", pullRequest.getTitle(), pullRequest.getHtmlUrl());
    }

    /**
     * Test that "Open Pull Request" is working.
     */
    @Test
    public void testFork()
    {
        String forkRepositoryName = repositoryName + "_fork";

        String pullRequestName = issueKey + ": Open PR";
        String[] commitNodeOpen = new String[2];

        gitResource.init(repositoryName, gitHubResource.getRepository(GitHubTestResource.USER, repositoryName).getCloneUrl());
        gitResource.addFile(repositoryName, "README.txt", "Hello World!".getBytes());
        gitResource.commit(repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        gitResource.push(repositoryName, GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD);

        gitHubResource.fork(GitHubTestResource.ORGANIZATION, GitHubTestResource.USER, repositoryName,
                GitHubTestResource.Lifetime.DURING_TEST_METHOD, EXPIRATION_DURATION_5_MIN);

        gitResource.addRepository(forkRepositoryName);
        gitResource.clone(forkRepositoryName, gitHubResource.getRepository(GitHubTestResource.ORGANIZATION, repositoryName).getCloneUrl(),
                GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD);

        gitResource.addFile(forkRepositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = gitResource.commit(forkRepositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        gitResource.addFile(forkRepositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = gitResource.commit(forkRepositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        gitResource.push(forkRepositoryName, GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD, "master");

        // seems that information for pull request are calculated asynchronously,
        // / it is not possible to create PR immediately after push
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            // nothing to do
        }

        PullRequest pullRequest = gitHubResource.openPullRequest(GitHubTestResource.USER, repositoryName, pullRequestName,
                "Open PR description", GitHubTestResource.ORGANIZATION + ":master", "master");

        AccountsPage accountsPage = jiraTestedProduct.visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(getAccountType(), GitHubTestResource.USER);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(repositoryName);
        repository.enable();
        repository.synchronize();

        RestDevResponse<RestPrRepository> pullRequestActual = pullRequestLocalRestpoint.getPullRequest(issueKey);
        Assert.assertEquals(pullRequestActual.getRepositories().size(), 1);
        Assert.assertEquals(pullRequestActual.getRepositories().get(0).getPullRequests().size(), 1);
        RestPullRequest actualPullRequest = pullRequestActual.getRepositories().get(0).getPullRequests().get(0);

        assertPullRequestInfo(actualPullRequest, "OPEN", pullRequest.getTitle(), pullRequest.getHtmlUrl());
    }

    private void assertPullRequestInfo(RestPullRequest pullRequest, String state, String name, String htmlUrl)
    {
        Assert.assertEquals(pullRequest.getStatus(), state);
        Assert.assertEquals(pullRequest.getTitle(), name);
        Assert.assertEquals(pullRequest.getUrl(), htmlUrl);
    }

    private void assertRestRef(RestRef ref, String owner, String repositoryName, String branch)
    {
        Assert.assertEquals(ref.getRepository(), owner + "/" + repositoryName);
        Assert.assertEquals(ref.getBranch(), branch);
    }

    private void assertRestUser(RestUser author, String userName, String name, String email)
    {
        Assert.assertEquals(author.getUsername(), PR_AUTHOR_USERNAME);
        Assert.assertEquals(author.getName(), PR_AUTHOR_NAME);
    }

}
