package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrCommit;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketUser;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import com.google.common.collect.Ordering;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount.AccountType;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.MercurialDvcs;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

import java.util.List;
import javax.annotation.Nullable;

/**
 * Pull request Bitbucket related tests.
 *
 * @author Miroslav Stencel
 */
public class PullRequestBitbucketDVCSTest extends AbstractBitbucketDVCSTest
{
    @Factory (dataProvider = "dvcs")
    public PullRequestBitbucketDVCSTest(Dvcs dvcs)
    {
        super(dvcs);
    }

    @DataProvider
    public static Object[][] dvcs()
    {
        return new Object[][]
                {
                        new Object[] { new MercurialDvcs() }
//                        new Object[] { new GitDvcs() }
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
     * Prefix of repository name used by tests of this class.
     */
    private static final String REPOSITORY_NAME_PREFIX = "it.restart." + PullRequestBitbucketDVCSTest.class.getSimpleName().toLowerCase();

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

    private String repositoryName;

    private static final int EXPIRATION_DURATION_5_MIN = 5 * 60 * 1000;

    /**
     * {@inheritDoc}
     */
    @BeforeMethod
    public void onTestSetup()
    {
        issueKey = addTestIssue(TEST_PROJECT_KEY, TEST_ISSUE_SUMMARY);
        repositoryName = timestampNameTestResource.randomName(REPOSITORY_NAME_PREFIX, EXPIRATION_DURATION_5_MIN);
        addTestRepository(ACCOUNT_NAME, repositoryName, PASSWORD);
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

        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, repositoryName, fixBranchName);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        BitbucketPullRequest pullRequest = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            // nop
        }

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
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
    }

    /**
     * Test that "Update Pull Request" is working. Adding new commits.
     */
    @Test
    public void testUpdateBranch()
    {
        String expectedPullRequestName = issueKey + ": Open PR";
        String[] expectedCommitNodeUpdate = new String[2];
        String[] expectedCommitNodeOpen = new String[2];

        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        String fixBranchName = issueKey + "_fix";
        createBranch(ACCOUNT_NAME, repositoryName, fixBranchName);

        // Assert PR Opened information
        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        expectedCommitNodeOpen[0] = commit(ACCOUNT_NAME, repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        expectedCommitNodeOpen[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        BitbucketPullRequest expectedPullRequest = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequestName, "Open PR description", fixBranchName,
                getDefaultBranchName());

        // Assert PR Updated information
        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix_update.txt", "Virtual fix - update {}".getBytes());
        expectedCommitNodeUpdate[0] = commit(ACCOUNT_NAME, repositoryName, "Fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix_update.txt", "Virtual fix - update \n {\n}".getBytes());
        expectedCommitNodeUpdate[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName);

        BitbucketPullRequest updatedPullRequest = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequestName, "Open PR description", fixBranchName,
                getDefaultBranchName());

        // test of synchronization
        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        account.synchronizeRepository(repositoryName);

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), expectedPullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.OPEN.toString());
        Assert.assertTrue(updatedPullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        List<RestPrCommit> restCommits = restPullRequest.getCommits();
        Assert.assertEquals(restCommits.get(0).getNode(), expectedCommitNodeUpdate[1]);
        Assert.assertEquals(restCommits.get(1).getNode(), expectedCommitNodeUpdate[0]);
        Assert.assertEquals(restCommits.get(2).getNode(), expectedCommitNodeOpen[1]);
        Assert.assertEquals(restCommits.get(3).getNode(), expectedCommitNodeOpen[0]);
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

        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, repositoryName, fixBranchName);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        BitbucketPullRequest pullRequest = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            // nop
        }

        declinePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequest);

        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        account.synchronizeRepository(repositoryName);

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.DECLINED.toString());
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
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

        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, repositoryName, fixBranchName);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        BitbucketPullRequest pullRequest = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
            // nop
        }

        approvePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequest);

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
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getParticipants().size(), 1);
        Assert.assertEquals(restPullRequest.getParticipants().get(0).getUser().getUsername(), ACCOUNT_NAME);
        Assert.assertTrue(restPullRequest.getParticipants().get(0).isApproved());
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

        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, repositoryName, fixBranchName);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        BitbucketPullRequest pullRequest = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            // nop
        }

        mergePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequest);

        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        account.synchronizeRepository(repositoryName);

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.MERGED.toString());
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
    }

    /**
     * Test that "Open Pull Request" is working on fork.
     */
    @Test
    public void testFork()
    {
        String pullRequestName = issueKey + ": Open PR";
        String[] commitNodeOpen = new String[2];

        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        BitbucketRepository forkedRepository = fork(ACCOUNT_NAME, repositoryName, FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);

        addFile(forkedRepository.getOwner(), forkedRepository.getSlug(), issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(forkedRepository.getOwner(), forkedRepository.getSlug(), "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(forkedRepository.getOwner(), forkedRepository.getSlug(), issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(forkedRepository.getOwner(), forkedRepository.getSlug(), "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(forkedRepository.getOwner(), forkedRepository.getSlug(), FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD, getDefaultBranchName());

        BitbucketPullRequest pullRequest = openForkPullRequest(ACCOUNT_NAME, repositoryName, pullRequestName, "Open PR description", getDefaultBranchName(),
                getDefaultBranchName(), FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);

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
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), FORK_ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getSource().getRepository(), FORK_ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
    }

    /**
     * Test that "Numer of Comments on Pull Request" is working.
     */
    @Test
    public void testCommentsPullRequestBranch()
    {
        String pullRequestName = issueKey + ": Open PR";
        String fixBranchName = issueKey + "_fix";
        String[] commitNodeOpen = new String[2];

        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, repositoryName, fixBranchName);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        BitbucketPullRequest pullRequest = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            // nop
        }

        commentPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequest, "Test comment 1");
        commentPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, pullRequest, "Test comment 2");

        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        account.synchronizeRepository(repositoryName);

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getCommentCount(), 2);
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.OPEN.toString());
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
    }

    /**
     * Test that "Update Pull Request" works.
     */
    @Test
    public void testUpdatePullRequest()
    {
        String expectedPullRequestName = issueKey + ": Open PR";
        String[] expectedCommitNodeUpdate = new String[2];
        String[] expectedCommitNodeOpen = new String[2];

        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        String stableBranch = "stable";
        createBranch(ACCOUNT_NAME, repositoryName, stableBranch);
        String stableCommit = commit(ACCOUNT_NAME, repositoryName, "Commit in stable", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, stableBranch, true);

        String fixBranchName = issueKey + "_fix";
        createBranch(ACCOUNT_NAME, repositoryName, fixBranchName);

        // Assert PR Opened information
        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        expectedCommitNodeOpen[0] = commit(ACCOUNT_NAME, repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        expectedCommitNodeOpen[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        BitbucketPullRequest expectedPullRequest = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequestName, "Open PR description", fixBranchName,
                stableBranch);

        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        account.synchronizeRepository(repositoryName);

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);
        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), expectedPullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.OPEN.toString());
        Assert.assertTrue(expectedPullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), stableBranch);
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        List<RestPrCommit> restCommits = restPullRequest.getCommits();
        MatcherAssert.assertThat(Lists.transform(restCommits, new Function<RestPrCommit, String>()
        {
            @Override
            public String apply(@Nullable final RestPrCommit restPrCommit)
            {
                return restPrCommit.getNode();
            }
        }), Matchers.containsInAnyOrder(expectedCommitNodeOpen));
        // Assert PR Updated information
        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix_update.txt", "Virtual fix - update {}".getBytes());
        expectedCommitNodeUpdate[0] = commit(ACCOUNT_NAME, repositoryName, "Fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix_update.txt", "Virtual fix - update \n {\n}".getBytes());
        expectedCommitNodeUpdate[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName);

        BitbucketUser reviewer = new BitbucketUser();
        reviewer.setUsername(FORK_ACCOUNT_NAME);
        expectedPullRequest.getReviewers().add(reviewer);

        BitbucketPullRequest updatedPullRequest = updatePullRequest(
                ACCOUNT_NAME,
                repositoryName,
                PASSWORD,
                expectedPullRequest,
                expectedPullRequestName + " updated",
                "Open PR description",
                getDefaultBranchName());

        account.synchronizeRepository(repositoryName);

        response = getPullRequestResponse(issueKey);

        Assert.assertEquals(response.getRepositories().size(), 1);
        restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), expectedPullRequestName + " updated");
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.OPEN.toString());
        Assert.assertTrue(updatedPullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + repositoryName);
        restCommits = restPullRequest.getCommits();
        List<String> commits = Lists.transform(restCommits, new Function<RestPrCommit, String>()
        {
            @Override
            public String apply(@Nullable final RestPrCommit restPrCommit)
            {
                return restPrCommit.getNode();
            }
        });
        commits.remove(stableCommit);
        MatcherAssert.assertThat(commits, Matchers.containsInAnyOrder(ObjectArrays.concat(expectedCommitNodeOpen, expectedCommitNodeUpdate, String.class)));
        Assert.assertEquals(restPullRequest.getParticipants().size(), 1);
        Assert.assertEquals(restPullRequest.getParticipants().get(0).getUser().getUsername(), FORK_ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getParticipants().get(0).getRole(), "REVIEWER");
    }

    /**
     * Test that "Multiple Pull Request" synchronization works.
     */
    @Test
    public void testMultiplePullRequests()
    {
        String expectedPullRequestName = issueKey + ": Open PR";

        String[] branch1Commits = new String[2];
        String[] branch2Commits = new String[2];
        String[] branch3Commits = new String[2];

        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        String branch1 = "branch1";
        createBranch(ACCOUNT_NAME, repositoryName, branch1);
        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        branch1Commits[0] = commit(ACCOUNT_NAME, repositoryName, "Fix branch1", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        branch1Commits[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix branch1", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, branch1, true);

        switchBranch(ACCOUNT_NAME, repositoryName, getDefaultBranchName());
        String branch2 = "branch2";
        createBranch(ACCOUNT_NAME, repositoryName, branch2);
        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix_2.txt", "Virtual fix {}".getBytes());
        branch2Commits[0] = commit(ACCOUNT_NAME, repositoryName, "Fix branch2", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix_2.txt", "Virtual fix \n{\n}".getBytes());
        branch2Commits[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix branch2", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, branch2, true);

        switchBranch(ACCOUNT_NAME, repositoryName, getDefaultBranchName());
        String branch3 = "branch3";
        createBranch(ACCOUNT_NAME, repositoryName, branch3);
        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix_3.txt", "Virtual fix {}".getBytes());
        branch3Commits[0] = commit(ACCOUNT_NAME, repositoryName, "Fix branch3", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix_3.txt", "Virtual fix \n{\n}".getBytes());
        branch3Commits[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix branch3", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, branch3, true);

        BitbucketPullRequest expectedPullRequest1 = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequestName + " " + branch1, "Open PR description", branch1,
                getDefaultBranchName());

        BitbucketPullRequest expectedPullRequest2 = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequestName + " " + branch2, "Open PR description", branch2,
                getDefaultBranchName());

        BitbucketPullRequest expectedPullRequest3 = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequestName + " " + branch3, "Open PR description", branch3,
                getDefaultBranchName());

        mergePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequest2);
        declinePullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequest3);

        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        account.synchronizeRepository(repositoryName);

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);
        Assert.assertEquals(response.getRepositories().size(), 1);

        RestPrRepository restPrRepository = response.getRepositories().get(0);

        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 3);

        List<RestPullRequest> restPullRequests = Ordering.natural().onResultOf(new Function<RestPullRequest, Long>()
        {
            @Override
            public Long apply(@Nullable final RestPullRequest restPullRequest)
            {
                return restPullRequest.getId();
            }
        }).sortedCopy(restPrRepository.getPullRequests());

        assertPullRequest(ACCOUNT_NAME, repositoryName, expectedPullRequest1, restPullRequests.get(0), expectedPullRequestName + " " + branch1, branch1Commits, branch1, PullRequestStatus.OPEN);
        assertPullRequest(ACCOUNT_NAME, repositoryName, expectedPullRequest2, restPullRequests.get(1), expectedPullRequestName + " " + branch2, branch2Commits, branch2, PullRequestStatus.MERGED);
        assertPullRequest(ACCOUNT_NAME, repositoryName, expectedPullRequest3, restPullRequests.get(2), expectedPullRequestName + " " + branch3, branch3Commits, branch3, PullRequestStatus.DECLINED);
    }

    /**
     * Test that "Pull Request" synchronization in multiple repositories works.
     */
    @Test
    public void testPullRequestsMultipleRepositories()
    {
        String anotherRepositoryName = timestampNameTestResource.randomName(REPOSITORY_NAME_PREFIX, EXPIRATION_DURATION_5_MIN);
        addTestRepository(ACCOUNT_NAME, anotherRepositoryName, PASSWORD);

        String expectedPullRequestName = issueKey + ": Open PR";

        String[] repository1Commits = new String[2];
        String[] repository2Commits = new String[2];

        // First repository
        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        String fixBranch1 = "fixbranch1";
        createBranch(ACCOUNT_NAME, repositoryName, fixBranch1);
        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        repository1Commits[0] = commit(ACCOUNT_NAME, repositoryName, "Fix repository1", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        repository1Commits[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix repository1", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranch1, true);

        BitbucketPullRequest expectedPullRequest1 = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequestName + repositoryName, "Open PR description", fixBranch1,
                getDefaultBranchName());

        // Second repository
        addFile(ACCOUNT_NAME, anotherRepositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, anotherRepositoryName, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(ACCOUNT_NAME, anotherRepositoryName, ACCOUNT_NAME, PASSWORD);

        String fixBranch2 = "fixbranch2";
        createBranch(ACCOUNT_NAME, anotherRepositoryName, fixBranch2);
        addFile(ACCOUNT_NAME, anotherRepositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        repository2Commits[0] = commit(ACCOUNT_NAME, anotherRepositoryName, "Fix repository2", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, anotherRepositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        repository2Commits[1] = commit(ACCOUNT_NAME, anotherRepositoryName, "Formatting fix repository2", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, anotherRepositoryName, ACCOUNT_NAME, PASSWORD, fixBranch2, true);

        BitbucketPullRequest expectedPullRequest2 = openPullRequest(ACCOUNT_NAME, anotherRepositoryName, PASSWORD, expectedPullRequestName + anotherRepositoryName, "Open PR description", fixBranch2,
                getDefaultBranchName());

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        account.synchronizeRepositories(repositoryName, anotherRepositoryName);
        accountsPage.waitForSyncToFinish();

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);
        Assert.assertEquals(response.getRepositories().size(), 2);

        RestPrRepository restPrRepository = response.getRepositories().get(0);

        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);

        assertPullRequest(ACCOUNT_NAME, repositoryName, expectedPullRequest1, restPrRepository.getPullRequests().get(0), expectedPullRequestName + repositoryName, repository1Commits, fixBranch1, PullRequestStatus.OPEN);

        restPrRepository = response.getRepositories().get(1);
        Assert.assertEquals(restPrRepository.getSlug(), anotherRepositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);

        assertPullRequest(ACCOUNT_NAME, anotherRepositoryName, expectedPullRequest2, restPrRepository.getPullRequests().get(0), expectedPullRequestName + anotherRepositoryName, repository2Commits, fixBranch2, PullRequestStatus.OPEN);
    }

    /**
     * Test that "Pull Request" synchronization in multiple accounts works.
     */
    @Test
    public void testPullRequestsMultipleAccounts()
    {
        String anotherRepositoryName = timestampNameTestResource.randomName(REPOSITORY_NAME_PREFIX, EXPIRATION_DURATION_5_MIN);
        addTestRepository(FORK_ACCOUNT_NAME, anotherRepositoryName, FORK_ACCOUNT_PASSWORD);

        String expectedPullRequestName = issueKey + ": Open PR";

        String[] repository1Commits = new String[2];
        String[] repository2Commits = new String[2];

        // First repository
        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        String fixBranch1 = "fixbranch";
        createBranch(ACCOUNT_NAME, repositoryName, fixBranch1);
        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        repository1Commits[0] = commit(ACCOUNT_NAME, repositoryName, "Fix repository1", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        repository1Commits[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix repository1", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranch1, true);

        BitbucketPullRequest expectedPullRequest1 = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequestName + repositoryName, "Open PR description", fixBranch1,
                getDefaultBranchName());

        // Second repository
        addFile(FORK_ACCOUNT_NAME, anotherRepositoryName, "README.txt", "Hello World!".getBytes());
        commit(FORK_ACCOUNT_NAME, anotherRepositoryName, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(FORK_ACCOUNT_NAME, anotherRepositoryName, FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);

        String fixBranch2 = "fixbranch2";
        createBranch(FORK_ACCOUNT_NAME, anotherRepositoryName, fixBranch2);
        addFile(FORK_ACCOUNT_NAME, anotherRepositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        repository2Commits[0] = commit(FORK_ACCOUNT_NAME, anotherRepositoryName, "Fix repository2", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(FORK_ACCOUNT_NAME, anotherRepositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        repository2Commits[1] = commit(FORK_ACCOUNT_NAME, anotherRepositoryName, "Formatting fix repository2", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(FORK_ACCOUNT_NAME, anotherRepositoryName, FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD, fixBranch2, true);

        BitbucketPullRequest expectedPullRequest2 = openPullRequest(FORK_ACCOUNT_NAME, anotherRepositoryName, PASSWORD, expectedPullRequestName + anotherRepositoryName, "Open PR description", fixBranch2,
                getDefaultBranchName());

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        AccountsPageAccount secondAccount = refreshAccount(FORK_ACCOUNT_NAME);
        account.synchronizeRepositories(repositoryName);
        secondAccount.synchronizeRepositories(anotherRepositoryName);
        accountsPage.waitForSyncToFinish();

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);
        Assert.assertEquals(response.getRepositories().size(), 2);

        RestPrRepository restPrRepository = response.getRepositories().get(0);

        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);

        assertPullRequest(ACCOUNT_NAME, repositoryName, expectedPullRequest1, restPrRepository.getPullRequests().get(0), expectedPullRequestName + repositoryName, repository1Commits, fixBranch1, PullRequestStatus.OPEN);

        restPrRepository = response.getRepositories().get(1);
        Assert.assertEquals(restPrRepository.getSlug(), anotherRepositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);

        assertPullRequest(FORK_ACCOUNT_NAME, anotherRepositoryName, expectedPullRequest2, restPrRepository.getPullRequests().get(0), expectedPullRequestName + anotherRepositoryName, repository2Commits, fixBranch2, PullRequestStatus.OPEN);
    }

    /**
     * Tests "Fullsync Pull Request"
     */
    @Test
    public void testFullSync()
    {
        String expectedPullRequestName = issueKey + ": Open PR";
        String[] expectedCommitNodes1 = new String[2];
        String[] expectedCommitNodes2 = new String[2];

        addFile(ACCOUNT_NAME, repositoryName, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, repositoryName, "Initial commit!", "Stanislav Dvorscak", "sdvorscak@atlassian.com");
        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);

        String fixBranchName1 = issueKey + "_fix1";
        createBranch(ACCOUNT_NAME, repositoryName, fixBranchName1);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        expectedCommitNodes1[0] = commit(ACCOUNT_NAME, repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        expectedCommitNodes1[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName1, true);

        BitbucketPullRequest expectedPullRequest1 = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequestName + fixBranchName1, "Open PR description", fixBranchName1,
                getDefaultBranchName());

        AccountsPageAccount account = refreshAccount(ACCOUNT_NAME);
        account.synchronizeRepository(repositoryName);

        RestDevResponse<RestPrRepository> response = getPullRequestResponse(issueKey);
        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);

        assertPullRequest(ACCOUNT_NAME, repositoryName, expectedPullRequest1, restPrRepository.getPullRequests().get(0), expectedPullRequestName + fixBranchName1, expectedCommitNodes1, fixBranchName1, PullRequestStatus.OPEN);

        // another PR
        switchBranch(ACCOUNT_NAME, repositoryName, getDefaultBranchName());
        String fixBranchName2 = issueKey + "_fix2";
        createBranch(ACCOUNT_NAME, repositoryName, fixBranchName2);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        expectedCommitNodes2[0] = commit(ACCOUNT_NAME, repositoryName, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, repositoryName, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        expectedCommitNodes2[1] = commit(ACCOUNT_NAME, repositoryName, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD, fixBranchName2, true);

        BitbucketPullRequest expectedPullRequest2 = openPullRequest(ACCOUNT_NAME, repositoryName, PASSWORD, expectedPullRequestName + fixBranchName2, "Open PR description", fixBranchName2,
                getDefaultBranchName());

        account.fullSynchronizeRepository(repositoryName);

        response = getPullRequestResponse(issueKey);
        Assert.assertEquals(response.getRepositories().size(), 1);

        restPrRepository = response.getRepositories().get(0);

        Assert.assertEquals(restPrRepository.getSlug(), repositoryName);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 2);

        assertPullRequest(ACCOUNT_NAME, repositoryName, expectedPullRequest1, restPrRepository.getPullRequests().get(1), expectedPullRequestName + fixBranchName1, expectedCommitNodes1, fixBranchName1, PullRequestStatus.OPEN);
        assertPullRequest(ACCOUNT_NAME, repositoryName, expectedPullRequest2, restPrRepository.getPullRequests().get(0), expectedPullRequestName + fixBranchName2, expectedCommitNodes2, fixBranchName2, PullRequestStatus.OPEN);
    }

    private void assertPullRequest(final String account, final String repositoryName, final BitbucketPullRequest pullRequest, final RestPullRequest restPullRequest, final String pullRequestTitle, final String[] commits, final String sourceBranch, final PullRequestStatus status)
    {
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestTitle);
        Assert.assertEquals(restPullRequest.getStatus(), status.toString());
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), account);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), sourceBranch);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), account + "/" + repositoryName);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), account + "/" + repositoryName);
        List<RestPrCommit> restCommits = restPullRequest.getCommits();
        MatcherAssert.assertThat(Lists.transform(restCommits, new Function<RestPrCommit, String>()
        {
            @Override
            public String apply(@Nullable final RestPrCommit restPrCommit)
            {
                return restPrCommit.getNode();
            }
        }), Matchers.containsInAnyOrder(commits));
    }

    private AccountsPageAccount refreshAccount(final String accountName)
    {
        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, accountName);
        account.refresh();

        return account;
    }

}
