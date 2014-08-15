package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseClientProvider;
import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount;
import it.restart.com.atlassian.jira.plugins.dvcs.test.GithubEnterpriseTests;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.GitHubDvcs;
import it.restart.com.atlassian.jira.plugins.dvcs.testClient.GitHubPullRequestClient;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.testng.annotations.AfterClass;

public class GithubEnterprisePRTest extends PullRequestTestCases
{
    private static final String GITHUB_ENTERPRISE_USER_AGENT = "jira-dvcs-plugin-test";

    private GitHubTestResource gitHubTestResource;

    public GithubEnterprisePRTest()
    {
    }

    @Override
    protected void beforeEachTestInitialisation(final JiraTestedProduct jiraTestedProduct)
    {
        gitHubTestResource = new GitHubTestResource(new MagicVisitor(jiraTestedProduct));
        gitHubTestResource.beforeClass();
        setupGitHubTestResource(gitHubTestResource);
        dvcs = new GitHubDvcs(gitHubTestResource);
        pullRequestClient = new GitHubPullRequestClient(gitHubTestResource);

        addOrganizations(jiraTestedProduct);
    }

    private void addOrganizations(final JiraTestedProduct jiraTestedProduct)
    {
        OAuth oAuth = gitHubTestResource.addOAuth(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL, jiraTestedProduct.getProductInstance().getBaseUrl(),
                GitHubTestResource.Lifetime.DURING_CLASS);

        new MagicVisitor(jiraTestedProduct).visit(GithubLoginPage.class, GithubEnterpriseTests.GITHUB_ENTERPRISE_URL).doLogin();
        OAuthCredentials oAuthCredentials = new OAuthCredentials(oAuth.key, oAuth.secret);
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.getPage().deleteAllOrganizations();

        RepositoriesPageController.AccountType accountType = RepositoriesPageController.AccountType.getGHEAccountType(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL);
        repositoriesPageController.addOrganization(accountType, GitHubTestResource.USER, oAuthCredentials, false);
        repositoriesPageController.addOrganization(accountType, GitHubTestResource.ORGANIZATION, oAuthCredentials, false);
    }

    protected void setupGitHubTestResource(GitHubTestResource gitHubTestResource)
    {
        GitHubClient gitHubClient = GithubEnterpriseClientProvider.createClient(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL, GITHUB_ENTERPRISE_USER_AGENT);
        gitHubClient.setCredentials(ACCOUNT_NAME, PASSWORD);
        gitHubTestResource.addOwner(ACCOUNT_NAME, gitHubClient);
        gitHubTestResource.addOwner(GitHubTestResource.ORGANIZATION, gitHubClient);

        GitHubClient gitHubClient2 = GithubEnterpriseClientProvider.createClient(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL, GITHUB_ENTERPRISE_USER_AGENT);
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
        return AccountsPageAccount.AccountType.GIT_HUB_ENTERPRISE;
    }
}
