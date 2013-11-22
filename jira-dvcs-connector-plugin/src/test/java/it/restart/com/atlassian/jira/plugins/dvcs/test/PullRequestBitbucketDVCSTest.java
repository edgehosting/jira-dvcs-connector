package it.restart.com.atlassian.jira.plugins.dvcs.test;

import com.atlassian.jira.pageobjects.pages.JiraLoginPage;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPullRequest;
import com.atlassian.jira.plugins.dvcs.remoterestpoint.PullRequestLocalRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.AuthProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.BasicAuthAuthProvider;
import it.restart.com.atlassian.jira.plugins.dvcs.bitbucket.BitbucketLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount.AccountType;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Factory;
import org.testng.annotations.Test;

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

        BitbucketPullRequest pullRequest = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, PASSWORD, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException e)
        {
            // nop
        }

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
        if (!repository.isEnabled())
        {
            repository.enable();
            repository.synchronize();
        } else
        {
            // we need to fullsync here because of the bug https://sdog.jira.com/browse/BBC-608
            repository.fullSynchronize();
        }

        RestDevResponse<RestPrRepository> response = getPullRequestResponse();

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), REPOSITORY_NAME);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), RepositoryPullRequestMapping.Status.OPEN.toString());
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
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

        BitbucketPullRequest expectedPullRequest = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, PASSWORD, expectedPullRequestName, "Open PR description", fixBranchName,
                getDefaultBranchName());

        // Assert PR Updated information
        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix_update.txt", "Virtual fix - update {}".getBytes());
        expectedCommitNodeUpdate[0] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix_update.txt", "Virtual fix - update \n {\n}".getBytes());
        expectedCommitNodeUpdate[1] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Formatting fix - update", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD, fixBranchName);

        BitbucketPullRequest updatedPullRequest = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, PASSWORD, expectedPullRequestName, "Open PR description", fixBranchName,
                getDefaultBranchName());

        // test of synchronization
        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
        if (!repository.isEnabled())
        {
            repository.enable();
            repository.synchronize();
        } else
        {
            // we need to fullsync here because of the bug https://sdog.jira.com/browse/BBC-608
            repository.fullSynchronize();
        }

        RestDevResponse<RestPrRepository> response = getPullRequestResponse();

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), REPOSITORY_NAME);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), expectedPullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), RepositoryPullRequestMapping.Status.OPEN.toString());
        Assert.assertTrue(updatedPullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
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

        BitbucketPullRequest pullRequest = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, PASSWORD, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException e)
        {
            // nop
        }

        declinePullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequest);

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
        if (!repository.isEnabled())
        {
            repository.enable();
            repository.synchronize();
        } else
        {
            // we need to fullsync here because of the bug https://sdog.jira.com/browse/BBC-608
            repository.fullSynchronize();
        }

        RestDevResponse<RestPrRepository> response = getPullRequestResponse();

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), REPOSITORY_NAME);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), RepositoryPullRequestMapping.Status.DECLINED.toString());
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
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

        BitbucketPullRequest pullRequest = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, PASSWORD, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(2000);
        } catch (InterruptedException e)
        {
            // nop
        }

        approvePullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequest);

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
        if (!repository.isEnabled())
        {
            repository.enable();
            repository.synchronize();
        } else
        {
            // we need to fullsync here because of the bug https://sdog.jira.com/browse/BBC-608
            repository.fullSynchronize();
        }

        RestDevResponse<RestPrRepository> response = getPullRequestResponse();

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), REPOSITORY_NAME);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), RepositoryPullRequestMapping.Status.OPEN.toString());
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
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

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, REPOSITORY_NAME, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, REPOSITORY_NAME, fixBranchName);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        BitbucketPullRequest pullRequest = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, PASSWORD, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException e)
        {
            // nop
        }

        mergePullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequest);

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
        if (!repository.isEnabled())
        {
            repository.enable();
            repository.synchronize();
        } else
        {
            // we need to fullsync here because of the bug https://sdog.jira.com/browse/BBC-608
            repository.fullSynchronize();
        }

        RestDevResponse<RestPrRepository> response = getPullRequestResponse();

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), REPOSITORY_NAME);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), RepositoryPullRequestMapping.Status.MERGED.toString());
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
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

        BitbucketPullRequest pullRequest = openForkPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, pullRequestName, "Open PR description", getDefaultBranchName(),
                getDefaultBranchName(), FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
        if (!repository.isEnabled())
        {
            repository.enable();
            repository.synchronize();
        } else
        {
            // we need to fullsync here because of the bug https://sdog.jira.com/browse/BBC-608
            repository.fullSynchronize();
        }

        RestDevResponse<RestPrRepository> response = getPullRequestResponse();

        Assert.assertEquals(response.getRepositories().size(), 1);

        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), REPOSITORY_NAME);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), RepositoryPullRequestMapping.Status.OPEN.toString());
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), FORK_ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getSource().getRepository(), FORK_ACCOUNT_NAME + "/" + REPOSITORY_NAME);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
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

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, "README.txt", "Hello World!".getBytes());
        commit(ACCOUNT_NAME, REPOSITORY_NAME, "Initial commit!", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);
        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD);

        createBranch(ACCOUNT_NAME, REPOSITORY_NAME, fixBranchName);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix {}".getBytes());
        commitNodeOpen[0] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        addFile(ACCOUNT_NAME, REPOSITORY_NAME, issueKey + "_fix.txt", "Virtual fix \n{\n}".getBytes());
        commitNodeOpen[1] = commit(ACCOUNT_NAME, REPOSITORY_NAME, "Formatting fix", COMMIT_AUTHOR, COMMIT_AUTHOR_EMAIL);

        push(ACCOUNT_NAME, REPOSITORY_NAME, ACCOUNT_NAME, PASSWORD, fixBranchName, true);

        BitbucketPullRequest pullRequest = openPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, PASSWORD, pullRequestName, "Open PR description", fixBranchName, getDefaultBranchName());

        // Give a time to Bitbucket after creation of pullRequest
        try
        {
            Thread.sleep(1000);
        } catch (InterruptedException e)
        {
            // nop
        }

        commentPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, PASSWORD, pullRequest, "Test comment 1");
        commentPullRequest(ACCOUNT_NAME, REPOSITORY_NAME, PASSWORD, pullRequest, "Test comment 2");

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.BITBUCKET, ACCOUNT_NAME);
        account.refresh();

        AccountsPageAccountRepository repository = account.getRepository(REPOSITORY_NAME);
        if (!repository.isEnabled())
        {
            repository.enable();
            repository.synchronize();
        } else
        {
            // we need to fullsync here because of the bug https://sdog.jira.com/browse/BBC-608
            repository.fullSynchronize();
        }

        RestDevResponse<RestPrRepository> response = getPullRequestResponse();

        Assert.assertEquals(response.getRepositories().size(), 1);
        RestPrRepository restPrRepository = response.getRepositories().get(0);
        Assert.assertEquals(restPrRepository.getSlug(), REPOSITORY_NAME);
        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);
        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertEquals(restPullRequest.getCommentCount(), 2);
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), RepositoryPullRequestMapping.Status.OPEN.toString());
        Assert.assertTrue(pullRequest.getLinks().getHtml().getHref().startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), ACCOUNT_NAME);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        Assert.assertEquals(restPullRequest.getSource().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), getDefaultBranchName());
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), ACCOUNT_NAME + "/" + REPOSITORY_NAME);
    }

    private RestDevResponse<RestPrRepository> getPullRequestResponse()
    {
        AuthProvider basicAuthProvider = new BasicAuthAuthProvider(null,
                JiraLoginPage.USER_ADMIN,
                JiraLoginPage.PASSWORD_ADMIN);
        PullRequestLocalRestpoint pullRequestLocalRest = new PullRequestLocalRestpoint(basicAuthProvider.provideRequestor());
        return pullRequestLocalRest.getPullRequest(getJiraTestedProduct().getProductInstance().getBaseUrl(), issueKey);
    }
}
