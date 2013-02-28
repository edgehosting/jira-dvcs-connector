package it.restart.com.atlassian.jira.plugins.dvcs.test;

import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount.AccountType;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccountRepository;
import it.restart.com.atlassian.jira.plugins.dvcs.page.issue.IssuePage;

import org.eclipse.egit.github.core.RepositoryId;
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
     * {@inheritDoc}
     */
    @Test
    public void testOpenPullRequestBranchToCleanMaster()
    {
        init(REPOSITORY_URI);
        addFile(REPOSITORY_URI, "README.txt", "Hello World!".getBytes());
        commit(REPOSITORY_URI, "Initial commit!");
        push(REPOSITORY_URI, USERNAME, PASSWORD);

        createBranch(REPOSITORY_URI, "open_pr");
        checkout(REPOSITORY_URI, "open_pr");
        addFile(REPOSITORY_URI, "open_pr.txt", "File of pull request.".getBytes());
        commit(REPOSITORY_URI, "Pull request commit");
        push(REPOSITORY_URI, USERNAME, PASSWORD, "open_pr");

        openPullRequest(REPOSITORY_URI, issueKey + ": Open PR", "Open PR description", "open_pr", "master");

        AccountsPage accountsPage = getJiraTestedProduct().visit(AccountsPage.class);
        AccountsPageAccount account = accountsPage.getAccount(AccountType.GitHub, USERNAME);
        account.refresh();

        RepositoryId repositoryId = RepositoryId.createFromId(REPOSITORY_URI);
        AccountsPageAccountRepository repository = account.getRepository(repositoryId.getName());
        repository.enable();
        repository.synchronize();

        IssuePage issuePage = getJiraTestedProduct().visit(IssuePage.class, issueKey);
        issuePage.openPRTab();
    }
}
