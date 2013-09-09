package it.restart.com.atlassian.jira.plugins.dvcs.test;

import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount.AccountType;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePagePullRequestTabActivity;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePagePullRequestTabActivityComment;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePagePullRequestTabActivityUpdate;

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;

/**
 * Pull request Bitbucket related tests.
 * 
 * @author Miroslav Stencel
 * 
 */
public class PullRequestBitbucketDVCSTest extends AbstractBitbucketDVCSTest
{
    @Factory(dataProvider = "dvcs")
    public PullRequestBitbucketDVCSTest(Dvcs dvcs)
    {
        super(dvcs);
    }

    @DataProvider
    public static Object[][] dvcs()
    {
        return new Object[][]
        {
            new Object[] { new MercurialDvcs() },
            new Object[] { new GitDvcs() }
        };
    }
    
    /**
     * Key of testing project.
     */
    private static final String TEST_PROJECT_KEY = "TST";

    /**
     * Summary of test issue.
     */
    private static final String TEST_ISSUE_SUMMARY = PullRequestBitbucketDVCSTest.class.getCanonicalName();

    /**
     * Name of repository used by tests of this class.
     */
    private static final String REPOSITORY_NAME = PullRequestBitbucketDVCSTest.class.getSimpleName().toLowerCase();

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
        addTestRepository(ACCOUNT_NAME, REPOSITORY_NAME, PASSWORD);
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

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, REPOSITORY_NAME, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, REPOSITORY_NAME, fixBranchName);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        String pullRequestUrl = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
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
        Assert.assertTrue(pullRequestUrl.startsWith(openedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), commitNodeOpen[1]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), commitNodeOpen[0]);
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

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, REPOSITORY_NAME, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, REPOSITORY_NAME, fixBranchName);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        String pullRequestUrl = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException e)
        {
            // nop
        }
        
        String comment = commentPullRequest(pullRequestUrl, "General Pull Request Comment");

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
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
        Assert.assertTrue(pullRequestUrl.startsWith(commentedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(commentedPullRequestActivity.getComment(), comment);
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

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, REPOSITORY_NAME, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD);

        String fixBranchName = issueKey + "_fix";
        createBranch(ACCOUNT_NAME, REPOSITORY_NAME, fixBranchName);

        // Assert PR Opened information
        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        expectedCommitNodeOpen[0] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        expectedCommitNodeOpen[1] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        String expectedPullRequestUrl = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, expectedPullRequestName, "Open PR description", fixBranchName,
                getDefaultBranchName());

        // Assert PR Updated information
        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix_update.txt", "Virtual fix - update {}".getBytes());
        expectedCommitNodeUpdate[0] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix_update.txt", "Virtual fix - update \n {\n}".getBytes());
        expectedCommitNodeUpdate[1] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Formatting fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD, fixBranchName);

        String updatedPullRequestUrl = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, expectedPullRequestName, "Open PR description", fixBranchName,
                getDefaultBranchName());
        
        // test of synchronization
        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
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
        Assert.assertTrue(expectedPullRequestUrl.startsWith(openedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), expectedCommitNodeOpen[1]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), expectedCommitNodeOpen[0]);

        // Updated PR
        IssuePagePullRequestTabActivityUpdate updatedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(1);
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestState(), "UPDATED");
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestName(), expectedPullRequestName);
        Assert.assertTrue(updatedPullRequestUrl.startsWith(updatedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(updatedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(updatedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), expectedCommitNodeUpdate[1]);
        assertCommitNode(updatedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), expectedCommitNodeUpdate[0]);
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

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, REPOSITORY_NAME, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, REPOSITORY_NAME, fixBranchName);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        String pullRequestUrl = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException e)
        {
            // nop
        }
        
        closePullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequestUrl);

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
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
        Assert.assertTrue(pullRequestUrl.startsWith(openedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), commitNodeOpen[1]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), commitNodeOpen[0]);

        // Assert PR declined activity information
        IssuePagePullRequestTabActivityUpdate declinedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(1);
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestState(), "DECLINED");
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestName(), pullRequestName);
        Assert.assertTrue(pullRequestUrl.startsWith(declinedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestCommits().size(), 0);
    }

    /**
     * Tests that "Approve Pull Request" is working.
     */
    @Test
    public void testApprove()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        String[] commitNodeOpen = new String[2];

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, REPOSITORY_NAME, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, REPOSITORY_NAME, fixBranchName);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        String pullRequestUrl = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException e)
        {
            // nop
        }
        
        approvePullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequestUrl);

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
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
        Assert.assertTrue(pullRequestUrl.startsWith(openedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), commitNodeOpen[1]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), commitNodeOpen[0]);

        // Assert PR declined activity information
        IssuePagePullRequestTabActivityUpdate declinedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(1);
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestState(), "APPROVED");
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestName(), pullRequestName);
        Assert.assertTrue(pullRequestUrl.startsWith(declinedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestCommits().size(), 0);
    }

    /**
     * Tests that "Merge Pull Request" is working.
     */
    @Test
    public void testMerge()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        String[] commitNodeOpen = new String[2];

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, REPOSITORY_NAME, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, REPOSITORY_NAME, fixBranchName);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        String pullRequestUrl = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException e)
        {
            // nop
        }
        
        mergePullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequestUrl);

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
        repository.enable();
        repository.synchronize();

        IssuePage issuePage = getJiraTestedProduct().visit(IssuePage.class, issueKey);
        issuePage.openPRTab();
        List<IssuePagePullRequestTabActivity> activities = issuePage.getIssuePagePullRequestTab().getActivities();

        Assert.assertEquals(activities.size(), 3);

        // Assert PR opened activity information
        IssuePagePullRequestTabActivityUpdate openedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(0);
        Assert.assertEquals(openedPullRequestActivity.getPullRequestState(), "OPENED");
        Assert.assertEquals(openedPullRequestActivity.getPullRequestName(), pullRequestName);
        Assert.assertTrue(pullRequestUrl.startsWith(openedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), commitNodeOpen[1]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), commitNodeOpen[0]);

        // Assert PR declined activity information
        IssuePagePullRequestTabActivityUpdate declinedPullRequestActivity = (IssuePagePullRequestTabActivityUpdate) activities.get(1);
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestState(), "MERGED");
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestName(), pullRequestName);
        Assert.assertTrue(pullRequestUrl.startsWith(declinedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(declinedPullRequestActivity.getPullRequestCommits().size(), 0);
    }
    
    /**
     * Test that "Open Pull Request" is working on fork.
     */
    @Test
    public void testFork()
    {
        String pullRequestName = issueKey + ": Open PR";
        String[] commitNodeOpen = new String[2];

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, REPOSITORY_NAME, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD);

        BitbucketRepository forkedRepository = fork(ACCOUNT_NAME, REPOSITORY_NAME, FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);

        addFile(forkedRepository.getOwner(), forkedRepository.getSlug(), issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(forkedRepository.getOwner(), forkedRepository.getSlug(), "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(forkedRepository.getOwner(), forkedRepository.getSlug(), issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(forkedRepository.getOwner(), forkedRepository.getSlug(), "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(forkedRepository.getOwner(), forkedRepository.getSlug(), FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD, getDefaultBranchName());

        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketLoginPage.class).doLogout();
        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketLoginPage.class).doLogin(FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);
        
        String pullRequestUrl = openForkPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequestName, "Open PR description", getDefaultBranchName(),
                getDefaultBranchName(), FORK_ACCOUNT_NAME);

        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketLoginPage.class).doLogout();
        new MagicVisitor(getJiraTestedProduct()).visit(BitbucketLoginPage.class).doLogin(ACCOUNT_NAME, PASSWORD);
        
        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
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
        Assert.assertTrue(pullRequestUrl.startsWith(openedPullRequestActivity.getPullRequestUrl()));
        Assert.assertEquals(openedPullRequestActivity.getPullRequestCommits().size(), 2);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(0).getCommitNode(), commitNodeOpen[1]);
        assertCommitNode(openedPullRequestActivity.getPullRequestCommits().get(1).getCommitNode(), commitNodeOpen[0]);
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
