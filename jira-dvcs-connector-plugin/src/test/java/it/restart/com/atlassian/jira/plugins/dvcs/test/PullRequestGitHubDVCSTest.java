package it.restart.com.atlassian.jira.plugins.dvcs.test;

import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount.AccountType;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePagePullRequestTabActivity;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePagePullRequestTabActivityComment;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePagePullRequestTabActivityUpdate;

import java.util.List;

import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Pull request GitHub related tests.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class PullRequestGitHubDVCSTest extends AbstractGitHubDVCSTest
{

    /**
     * Key of testing project.
     */
    private static final String TEST_PROJECT_KEY = "TST";

    /**
     * Summary of test issue.
     */
    private static final String TEST_ISSUE_SUMMARY = PullRequestGitHubDVCSTest.class.getCanonicalName();

    /**
     * Name of author which will be used as committer.
     */
    private static final String COMMIT_AUTHOR = "Stanislav Dvorscak";

    /**
     * Email of author which will be used as committer.
     */
    private static final String COMMIT_AUTHOR_EMAIL = "sdvorscak@atlassian.com";

    /**
     * Name of repository used by tests of this class.
     */
    private String repositoryUri;

    /**
     * Issue key used for testing.
     */
    private String issueKey;

    // implementation of abstract methods

    /**
     * {@inheritDoc}
     */
    @Override
    protected void signInGitHub()
    {
        new MagicVisitor(getJiraTestedProduct()).visit(GithubLoginPage.class).doLogin();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void signOutGitHub()
    {
        new MagicVisitor(getJiraTestedProduct()).visit(GithubLoginPage.class).doLogout();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected OAuth createOAuthSettings()
    {
        GithubOAuthPage GitHubOAuthPage = new MagicVisitor(getJiraTestedProduct()).visit(GithubOAuthPage.class);
        OAuth result = GitHubOAuthPage.addConsumer(getJiraTestedProduct().getProductInstance().getBaseUrl());
        // getJiraTestedProduct().visit(JiraGithubOAuthPage.class).setCredentials(result.key, result.secret);
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void destroyOAuthSettings(OAuth oAuth)
    {
        GithubOAuthPage gitHubOAuthPage = new MagicVisitor(getJiraTestedProduct()).visit(oAuth.applicationId, GithubOAuthPage.class);
        gitHubOAuthPage.removeConsumer();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected GitHubClient createGitHubClient()
    {
        GitHubClient result = GitHubClient.createClient("https://github.com/");
        result.setCredentials(getUsername(), getPassword());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addDVCSOrganizations()
    {
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(getJiraTestedProduct());
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.GITHUB, getUsername(), getOAuthCredentials(), false);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.GITHUB, getOrganization(), getOAuthCredentials(), false);
    }

    // end of: implementation of abstract methods

    /**
     * {@inheritDoc}
     */
    @BeforeMethod
    public void onTestSetup()
    {
        repositoryUri = getUsername() + "/" + PullRequestGitHubDVCSTest.class.getCanonicalName();
        issueKey = addTestIssue(TEST_PROJECT_KEY, TEST_ISSUE_SUMMARY);
        addTestRepository(repositoryUri);
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

        init(repositoryUri);
        addFile(repositoryUri, "README.txt", "Hello World!".getBytes());
        commit(repositoryUri, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(repositoryUri, getUsername(), getPassword());

        createBranch(repositoryUri, fixBranchName);
        checkout(repositoryUri, fixBranchName);

        addFile(repositoryUri, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(repositoryUri, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(repositoryUri, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(repositoryUri, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(repositoryUri, getUsername(), getPassword(), fixBranchName);

        PullRequest pullRequest = openPullRequest(repositoryUri, pullRequestName, "Open PR description", fixBranchName, "master");

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.GIT_HUB, getUsername());
        account.refresh();

        RepositoryId repositoryId = RepositoryId.createFromId(repositoryUri);
        AccountsPageAccountRepository repository = account.getRepository(repositoryId.getName());
        repository.enable();
        repository.synchronize();

        IssuePage issuePage = getJiraTestedProduct().visit(IssuePage.class, issueKey);
        issuePage.openPRTab();
        List<IssuePagePullRequestTabActivity> activities = issuePage.getIssuePagePullRequestTab().getActivities();

        Assert.assertEquals(activities.size(), 1);
        IssuePagePullRequestTabActivityUpdate openedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(0);

        // Assert PR information
        Assert.assertEquals(openedPullRequestActivity.getPullRequestState(), "OPENED");
        Assert.assertEquals(openedPullRequestActivity.getPullRequestName(), pullRequestName);
        Assert.assertEquals(openedPullRequestActivity.getPullRequestUrl(), pullRequest.getHtmlUrl());
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), commitNodeOpen[0]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), commitNodeOpen[1]);
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

        init(repositoryUri);
        addFile(repositoryUri, "README.txt", "Hello World!".getBytes());
        commit(repositoryUri, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(repositoryUri, getUsername(), getPassword());

        createBranch(repositoryUri, fixBranchName);
        checkout(repositoryUri, fixBranchName);

        addFile(repositoryUri, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(repositoryUri, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(repositoryUri, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(repositoryUri, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(repositoryUri, getUsername(), getPassword(), fixBranchName);

        PullRequest pullRequest = openPullRequest(repositoryUri, pullRequestName, "Open PR description", fixBranchName, "master");

        // wait until pull request will be established
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            // nothing to do
        }

        Comment comment = commentPullRequest(repositoryUri, pullRequest, "General Pull Request Comment");

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.GIT_HUB, getUsername());
        account.refresh();

        RepositoryId repositoryId = RepositoryId.createFromId(repositoryUri);
        AccountsPageAccountRepository repository = account.getRepository(repositoryId.getName());
        repository.enable();
        repository.synchronize();

        IssuePage issuePage = getJiraTestedProduct().visit(IssuePage.class, issueKey);
        issuePage.openPRTab();
        List<IssuePagePullRequestTabActivity> activities = issuePage.getIssuePagePullRequestTab().getActivities();

        Assert.assertEquals(activities.size(), 2);
        IssuePagePullRequestTabActivityComment commentedPullRequestActivity = (IssuePagePullRequestTabActivityComment) activities.get(1);

        // Assert PR information
        Assert.assertEquals(commentedPullRequestActivity.getPullRequestState(), "COMMENTED");
        Assert.assertEquals(commentedPullRequestActivity.getPullRequestName(), pullRequestName);
        Assert.assertEquals(commentedPullRequestActivity.getPullRequestUrl(), pullRequest.getHtmlUrl());
        Assert.assertEquals(commentedPullRequestActivity.getComment(), comment.getBody());
    }

    /**
     * Test that "Update Pull Request" is working.
     */
    @Test
    public void testUpdateBranch()
    {
        String expectedPullRequestName = issueKey + ": Open PR";
        String[] expectedCommitNodeUpdate = new String[2];
        String[] expectedCommitNodeOpen = new String[2];

        init(repositoryUri);
        addFile(repositoryUri, "README.txt", "Hello World!".getBytes());
        commit(repositoryUri, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(repositoryUri, getUsername(), getPassword());

        String fixBranchName = issueKey + "_fix";
        createBranch(repositoryUri, fixBranchName);
        checkout(repositoryUri, fixBranchName);

        // Assert PR Opened information
        addFile(repositoryUri, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        expectedCommitNodeOpen[0] = commit(repositoryUri, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(repositoryUri, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        expectedCommitNodeOpen[1] = commit(repositoryUri, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(repositoryUri, getUsername(), getPassword(), fixBranchName);

        PullRequest expectedPullRequest = openPullRequest(repositoryUri, expectedPullRequestName, "Open PR description", fixBranchName,
                "master");

        // Assert PR Updated information
        addFile(repositoryUri, issueKey + "_fix_update.txt", "Virtual fix - update {}".getBytes());
        expectedCommitNodeUpdate[0] = commit(repositoryUri, "Fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(repositoryUri, issueKey + "_fix_update.txt", "Virtual fix - update \n {\n}".getBytes());
        expectedCommitNodeUpdate[1] = commit(repositoryUri, "Formatting fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(repositoryUri, getUsername(), getPassword(), fixBranchName);

        // test of synchronization
        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.GIT_HUB, getUsername());
        account.refresh();

        RepositoryId repositoryId = RepositoryId.createFromId(repositoryUri);
        AccountsPageAccountRepository repository = account.getRepository(repositoryId.getName());
        repository.enable();
        repository.synchronize();

        IssuePage issuePage = getJiraTestedProduct().visit(IssuePage.class, issueKey);
        issuePage.openPRTab();
        List<IssuePagePullRequestTabActivity> activities = issuePage.getIssuePagePullRequestTab().getActivities();

        Assert.assertEquals(activities.size(), 2);

        // Opened PR
        IssuePagePullRequestTabActivityUpdate openedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(0);
        Assert.assertEquals(openedPullRequestActivity.getPullRequestState(), "OPENED");
        Assert.assertEquals(openedPullRequestActivity.getPullRequestName(), expectedPullRequestName);
        Assert.assertEquals(openedPullRequestActivity.getPullRequestUrl(), expectedPullRequest.getHtmlUrl());
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), expectedCommitNodeOpen[0]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), expectedCommitNodeOpen[1]);

        // Updated PR
        IssuePagePullRequestTabActivityUpdate updatedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(1);
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestState(), "UPDATED");
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestName(), expectedPullRequestName);
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestUrl(), expectedPullRequest.getHtmlUrl());
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(updatedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), expectedCommitNodeUpdate[0]);
        assertCommitNode(updatedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), expectedCommitNodeUpdate[1]);
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

        init(repositoryUri);
        addFile(repositoryUri, "README.txt", "Hello World!".getBytes());
        commit(repositoryUri, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(repositoryUri, getUsername(), getPassword());

        createBranch(repositoryUri, fixBranchName);
        checkout(repositoryUri, fixBranchName);

        addFile(repositoryUri, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(repositoryUri, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(repositoryUri, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(repositoryUri, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(repositoryUri, getUsername(), getPassword(), fixBranchName);

        PullRequest pullRequest = openPullRequest(repositoryUri, pullRequestName, "Open PR description", fixBranchName, "master");

        // gives such time for pull request creation
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            // nothing to do
        }

        closePullRequest(repositoryUri, pullRequest);

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.GIT_HUB, getUsername());
        account.refresh();

        RepositoryId repositoryId = RepositoryId.createFromId(repositoryUri);
        AccountsPageAccountRepository repository = account.getRepository(repositoryId.getName());
        repository.enable();
        repository.synchronize();

        IssuePage issuePage = getJiraTestedProduct().visit(IssuePage.class, issueKey);
        issuePage.openPRTab();
        List<IssuePagePullRequestTabActivity> activities = issuePage.getIssuePagePullRequestTab().getActivities();

        Assert.assertEquals(activities.size(), 2);

        // Assert PR opened activity information
        IssuePagePullRequestTabActivityUpdate openedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(0);
        Assert.assertEquals(openedPullRequestActivity.getPullRequestState(), "OPENED");
        Assert.assertEquals(openedPullRequestActivity.getPullRequestName(), pullRequestName);
        Assert.assertEquals(openedPullRequestActivity.getPullRequestUrl(), pullRequest.getHtmlUrl());
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), commitNodeOpen[0]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), commitNodeOpen[1]);

        // Assert PR declined activity information
        IssuePagePullRequestTabActivityUpdate declinedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(1);
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestState(), "DECLINED");
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestName(), pullRequestName);
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestUrl(), pullRequest.getHtmlUrl());
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestCommits().size(), 0);
    }

    /**
     * Test that "Open Pull Request" is working.
     */
    @Test
    public void testFork()
    {
        String pullRequestName = issueKey + ": Open PR";
        String[] commitNodeOpen = new String[2];

        init(repositoryUri);
        addFile(repositoryUri, "README.txt", "Hello World!".getBytes());
        commit(repositoryUri, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(repositoryUri, getUsername(), getPassword());

        String forkedRepositoryUri = fork(repositoryUri);
        clone(forkedRepositoryUri, getUsername(), getPassword());

        addFile(forkedRepositoryUri, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(forkedRepositoryUri, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(forkedRepositoryUri, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(forkedRepositoryUri, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(forkedRepositoryUri, getUsername(), getPassword(), "master");

        // seems that information for pull request are calculated asynchronously,
        // / it is not possible to create PR immediately after push
        try
        {
            Thread.sleep(5000);
        } catch (InterruptedException e)
        {
            // nothing to do
        }

        PullRequest pullRequest = openPullRequest(repositoryUri, pullRequestName, "Open PR description", getOrganization() + ":master",
                "master");

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.GIT_HUB, getUsername());
        account.refresh();

        RepositoryId repositoryId = RepositoryId.createFromId(repositoryUri);
        AccountsPageAccountRepository repository = account.getRepository(repositoryId.getName());
        repository.enable();
        repository.synchronize();

        IssuePage issuePage = getJiraTestedProduct().visit(IssuePage.class, issueKey);
        issuePage.openPRTab();
        List<IssuePagePullRequestTabActivity> activities = issuePage.getIssuePagePullRequestTab().getActivities();

        Assert.assertEquals(activities.size(), 1);
        IssuePagePullRequestTabActivityUpdate openedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(0);

        // Assert PR information
        Assert.assertEquals(openedPullRequestActivity.getPullRequestState(), "OPENED");
        Assert.assertEquals(openedPullRequestActivity.getPullRequestName(), pullRequestName);
        Assert.assertEquals(openedPullRequestActivity.getPullRequestUrl(), pullRequest.getHtmlUrl());
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), commitNodeOpen[0]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), commitNodeOpen[1]);
    }

    /**
     * Assert commit node - expected node must start with actual node.
     * 
     * @param actual
     *            node of commit
     * @param expected
     *            node of commit
     */
    private void assertCommitNode(String actual, String expected)
    {
        Assert.assertTrue(expected.startsWith(actual), "Expected that '" + expected + "' node, starts with '" + actual + "'");
    }

}

