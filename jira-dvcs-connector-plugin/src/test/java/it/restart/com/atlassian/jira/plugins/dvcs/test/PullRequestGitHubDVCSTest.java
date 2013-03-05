package it.restart.com.atlassian.jira.plugins.dvcs.test;

import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount.AccountType;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePagePullRequestTabActivity;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePagePullRequestTabActivityUpdate;

import java.util.List;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
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
     * Name of repository used by tests of this class.
     */
    private static final String REPOSITORY_URI = USERNAME + "/" + PullRequestGitHubDVCSTest.class.getCanonicalName();

    /**
     * Name of author which will be used as committer.
     */
    private static final String COMMIT_AUTHOR = "Stanislav Dvorscak";

    /**
     * Email of author which will be used as committer.
     */
    private static final String COMMIT_AUTHOR_EMAIL = "sdvorscak@atlassian.com";

    /**
     * Issue key used for testing.
     */
    private String issueKey;

    /**
     * {@inheritDoc}
     */
    @BeforeMethod
    public void onTestSetup()
    {
        issueKey = addTestIssue(TEST_PROJECT_KEY, TEST_ISSUE_SUMMARY);
        addTestRepository(REPOSITORY_URI);
    }

    /**
     * Test that "Open Pull Request" is working.
     */
    @Test
    public void testOpenPullRequestBranch()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        String[] commitNodeOpen = new String[2];

        init(REPOSITORY_URI);
        addFile(REPOSITORY_URI, "README.txt", "Hello World!".getBytes());
        commit(REPOSITORY_URI, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(REPOSITORY_URI, USERNAME, PASSWORD);

        createBranch(REPOSITORY_URI, fixBranchName);
        checkout(REPOSITORY_URI, fixBranchName);

        addFile(REPOSITORY_URI, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(REPOSITORY_URI, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(REPOSITORY_URI, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(REPOSITORY_URI, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(REPOSITORY_URI, USERNAME, PASSWORD, fixBranchName);

        PullRequest pullRequest = openPullRequest(REPOSITORY_URI, pullRequestName, "Open PR description", fixBranchName, "master");

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.GitHub, USERNAME);
        account.refresh();

        RepositoryId repositoryId = RepositoryId.createFromId(REPOSITORY_URI);
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
     * Test that "Update Pull Request" is working.
     */
    @Test(dependsOnMethods = "testOpenPullRequestBranch")
    private void testUpdatePullRequestBranch()
    {
        String expectedPullRequestName = issueKey + ": Open PR";
        String[] expectedCommitNodeUpdate = new String[2];
        String[] expectedCommitNodeOpen = new String[2];

        init(REPOSITORY_URI);
        addFile(REPOSITORY_URI, "README.txt", "Hello World!".getBytes());
        commit(REPOSITORY_URI, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(REPOSITORY_URI, USERNAME, PASSWORD);

        String fixBranchName = issueKey + "_fix";
        createBranch(REPOSITORY_URI, fixBranchName);
        checkout(REPOSITORY_URI, fixBranchName);

        // Assert PR Opened information
        addFile(REPOSITORY_URI, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        expectedCommitNodeOpen[0] = commit(REPOSITORY_URI, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(REPOSITORY_URI, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        expectedCommitNodeOpen[1] = commit(REPOSITORY_URI, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(REPOSITORY_URI, USERNAME, PASSWORD, fixBranchName);

        PullRequest expectedPullRequest = openPullRequest(REPOSITORY_URI, expectedPullRequestName, "Open PR description", fixBranchName, "master");

        // Assert PR Updated information
        addFile(REPOSITORY_URI, issueKey + "_fix_update.txt", "Virtual fix - update {}".getBytes());
        expectedCommitNodeUpdate[0] = commit(REPOSITORY_URI, "Fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(REPOSITORY_URI, issueKey + "_fix_update.txt", "Virtual fix - update \n {\n}".getBytes());
        expectedCommitNodeUpdate[1] = commit(REPOSITORY_URI, "Formatting fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(REPOSITORY_URI, USERNAME, PASSWORD, fixBranchName);

        // test of synchronization
        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.GitHub, USERNAME);
        account.refresh();

        RepositoryId repositoryId = RepositoryId.createFromId(REPOSITORY_URI);
        AccountsPageAccountRepository repository = account.getRepository(repositoryId.getName());
        repository.enable();
        repository.synchronize();

        IssuePage issuePage = getJiraTestedProduct().visit(IssuePage.class, issueKey);
        issuePage.openPRTab();
        List<IssuePagePullRequestTabActivity> activities = issuePage.getIssuePagePullRequestTab().getActivities();

        Assert.assertEquals(activities.size(), 2);

        // reverse order - ordered by date descending
        // Updated PR
        IssuePagePullRequestTabActivityUpdate updatedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(0);
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestState(), "UPDATED");
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestName(), expectedPullRequestName);
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestUrl(), expectedPullRequest.getHtmlUrl());
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(updatedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), expectedCommitNodeUpdate[0]);
        assertCommitNode(updatedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), expectedCommitNodeUpdate[1]);

        // Opened PR
        IssuePagePullRequestTabActivityUpdate openedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(1);
        Assert.assertEquals(openedPullRequestActivity.getPullRequestState(), "OPENED");
        Assert.assertEquals(openedPullRequestActivity.getPullRequestName(), expectedPullRequestName);
        Assert.assertEquals(openedPullRequestActivity.getPullRequestUrl(), expectedPullRequest.getHtmlUrl());
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), expectedCommitNodeOpen[0]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), expectedCommitNodeOpen[1]);
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
