package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestSupport;
import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.GitHubDvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.GitHubPullRequestClient;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.client.GitHubClient;

public class GithubPRTest extends PullRequestTestCases<PullRequest>
{
    private GitHubTestSupport gitHubTestSupport;

    public GithubPRTest()
    {
    }

    @Override
    protected void beforeEachTestClassInitialisation(final JiraTestedProduct jiraTestedProduct)
    {
        gitHubTestSupport = new GitHubTestSupport(new MagicVisitor(jiraTestedProduct));
        gitHubTestSupport.beforeClass();
        setupGitHubTestResource(gitHubTestSupport);

        dvcs = new GitHubDvcs(gitHubTestSupport);
        pullRequestClient = new GitHubPullRequestClient(gitHubTestSupport);

        addOrganizations(jiraTestedProduct);
    }

    @Override
    protected void cleanupAfterClass()
    {
        if (gitHubTestSupport != null)
        {
            gitHubTestSupport.afterClass();
        }
    }

    private void addOrganizations(final JiraTestedProduct jiraTestedProduct)
    {
        oAuth = gitHubTestSupport.addOAuth(GitHubTestSupport.URL, jiraTestedProduct.getProductInstance().getBaseUrl(),
                GitHubTestSupport.Lifetime.DURING_CLASS);

        new MagicVisitor(jiraTestedProduct).visit(GithubLoginPage.class).doLogin();
        OAuthCredentials oAuthCredentials = new OAuthCredentials(oAuth.key, oAuth.secret);
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.getPage().deleteAllOrganizations();
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.GITHUB, ACCOUNT_NAME,
                oAuthCredentials, false);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.GITHUB, GitHubTestSupport.ORGANIZATION,
                oAuthCredentials, false);
    }

    protected void setupGitHubTestResource(GitHubTestSupport gitHubTestSupport)
    {
        GitHubClient gitHubClient = GitHubClient.createClient(GitHubTestSupport.URL);
        gitHubClient.setCredentials(ACCOUNT_NAME, PASSWORD);
        gitHubTestSupport.addOwner(ACCOUNT_NAME, gitHubClient);
        gitHubTestSupport.addOwner(GitHubTestSupport.ORGANIZATION, gitHubClient);

        GitHubClient gitHubClient2 = GitHubClient.createClient(GitHubTestSupport.URL);
        gitHubClient2.setCredentials(FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);
        gitHubTestSupport.addOwner(FORK_ACCOUNT_NAME, gitHubClient2);
    }

    @Override
    protected void initLocalTestRepository()
    {
        gitHubTestSupport.beforeMethod();
        gitHubTestSupport.addRepositoryByName(ACCOUNT_NAME, repositoryName,
                GitHubTestSupport.Lifetime.DURING_TEST_METHOD, EXPIRATION_DURATION_5_MIN);

        dvcs.createTestLocalRepository(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);
    }

    @Override
    protected void cleanupLocalTestRepository()
    {
        gitHubTestSupport.afterMethod();
        dvcs.deleteAllRepositories();
    }

    @Override
    protected AccountsPageAccount.AccountType getAccountType()
    {
        return AccountsPageAccount.AccountType.GIT_HUB;
    }
}
