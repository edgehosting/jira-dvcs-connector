package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.GitHubDvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.GitHubPullRequestClient;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.PullRequestClient;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.testng.annotations.AfterClass;

public class GithubPRTest extends PullRequestTestCases
{
    private GitHubTestResource gitHubTestResource;

    public GithubPRTest()
    {
    }

    @Override
    protected String getTestIssueSummary()
    {
        return GithubPRTest.class.getCanonicalName();
    }

    @Override
    protected String getRepositoryNameSuffix()
    {
        return GithubPRTest.class.getSimpleName().toLowerCase();
    }

    @Override
    protected Dvcs getDvcs()
    {
        gitHubTestResource = new GitHubTestResource(new MagicVisitor(getJiraTestedProduct()));
        gitHubTestResource.beforeClass();
        setupGitHubTestResource(gitHubTestResource);

        return new GitHubDvcs(gitHubTestResource);
    }

    @Override
    protected PullRequestClient getPullRequestClient()
    {
        return new GitHubPullRequestClient(gitHubTestResource);
    }

    @Override
    protected void addOrganizations()
    {
        OAuth oAuth = gitHubTestResource.addOAuth(GitHubTestResource.URL, getJiraTestedProduct().getProductInstance().getBaseUrl(),
                GitHubTestResource.Lifetime.DURING_CLASS);

        new MagicVisitor(getJiraTestedProduct()).visit(GithubLoginPage.class).doLogin();
        OAuthCredentials oAuthCredentials = new OAuthCredentials(oAuth.key, oAuth.secret);
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(getJiraTestedProduct());
        repositoriesPageController.getPage().deleteAllOrganizations();
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.GITHUB, ACCOUNT_NAME,
                oAuthCredentials, false);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.GITHUB, GitHubTestResource.ORGANIZATION,
                oAuthCredentials, false);
    }

    protected void setupGitHubTestResource(GitHubTestResource gitHubTestResource)
    {
        GitHubClient gitHubClient = GitHubClient.createClient(GitHubTestResource.URL);
        gitHubClient.setCredentials(ACCOUNT_NAME, PASSWORD);
        gitHubTestResource.addOwner(ACCOUNT_NAME, gitHubClient);
        gitHubTestResource.addOwner(GitHubTestResource.ORGANIZATION, gitHubClient);

        GitHubClient gitHubClient2 = GitHubClient.createClient(GitHubTestResource.URL);
        gitHubClient2.setCredentials(FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);
        gitHubTestResource.addOwner(FORK_ACCOUNT_NAME, gitHubClient2);
    }

    @AfterClass (alwaysRun = true)
    protected void afterClass()
    {
        if (gitHubTestResource != null)
        {
            gitHubTestResource.afterClass();
        }
    }

    @Override
    protected void initLocalTestRepository()
    {
        gitHubTestResource.beforeMethod();
        repositoryName = gitHubTestResource.addRepositoryByName(ACCOUNT_NAME, repositoryName,
                GitHubTestResource.Lifetime.DURING_TEST_METHOD, EXPIRATION_DURATION_5_MIN);

        dvcs.createTestLocalRepository(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);
    }

    @Override
    protected void cleanupLocalTestRepository()
    {
        gitHubTestResource.afterMethod();
        dvcs.deleteAllRepositories();
    }

    @Override
    protected AccountsPageAccount.AccountType getAccountType()
    {
        return AccountsPageAccount.AccountType.GIT_HUB;
    }
}
