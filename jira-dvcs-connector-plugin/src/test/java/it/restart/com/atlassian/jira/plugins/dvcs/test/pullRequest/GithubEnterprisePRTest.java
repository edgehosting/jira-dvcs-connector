package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

/**
 * This class is commented out as it is flaky. If you are going to enable this it needs to be updated to use a
 * #it.restart.com.atlassian.jira.plugins.dvcs.testClient.RepositoryTestHelper built for GitHub
 */
public class GithubEnterprisePRTest //extends PullRequestTestCases<PullRequest>
{
//    private static final String GITHUB_ENTERPRISE_USER_AGENT = "jira-dvcs-plugin-test";
//
//    private GitHubTestSupport gitHubTestSupport;
//
//    public GithubEnterprisePRTest()
//    {
//    }
//
//    @Override
//    protected void beforeEachTestClassInitialisation(final JiraTestedProduct jiraTestedProduct)
//    {
//        gitHubTestSupport = new GitHubTestSupport(new MagicVisitor(jiraTestedProduct));
//        gitHubTestSupport.beforeClass();
//        setupGitHubTestResource(gitHubTestSupport);
//        dvcs = new GitHubDvcs(gitHubTestSupport);
//        pullRequestClient = new GitHubPullRequestClient(gitHubTestSupport);
//
//        addOrganizations(jiraTestedProduct);
//    }
//
//    @Override
//    protected void cleanupAfterClass()
//    {
//        if (gitHubTestSupport != null)
//        {
//            gitHubTestSupport.afterClass();
//        }
//    }
//
//    private void addOrganizations(final JiraTestedProduct jiraTestedProduct)
//    {
//        oAuth = gitHubTestSupport.addOAuth(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL, jiraTestedProduct.getProductInstance().getBaseUrl(),
//                GitHubTestSupport.Lifetime.DURING_CLASS);
//
//        new MagicVisitor(jiraTestedProduct).visit(GithubLoginPage.class, GithubEnterpriseTests.GITHUB_ENTERPRISE_URL).doLogin();
//        OAuthCredentials oAuthCredentials = new OAuthCredentials(oAuth.key, oAuth.secret);
//        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
//        repositoriesPageController.getPage().deleteAllOrganizations();
//
//        RepositoriesPageController.AccountType accountType = RepositoriesPageController.AccountType.getGHEAccountType(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL);
//        repositoriesPageController.addOrganization(accountType, IntegrationTestUserDetails.ACCOUNT_NAME, oAuthCredentials, false);
//        repositoriesPageController.addOrganization(accountType, GitHubTestSupport.ORGANIZATION, oAuthCredentials, false);
//    }
//
//    protected void setupGitHubTestResource(GitHubTestSupport gitHubTestSupport)
//    {
//        GitHubClient gitHubClient = GithubEnterpriseClientProvider.createClient(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL, GITHUB_ENTERPRISE_USER_AGENT);
//        gitHubClient.setCredentials(ACCOUNT_NAME, PASSWORD);
//        gitHubTestSupport.addOwner(ACCOUNT_NAME, gitHubClient);
//        gitHubTestSupport.addOwner(GitHubTestSupport.ORGANIZATION, gitHubClient);
//
//        GitHubClient gitHubClient2 = GithubEnterpriseClientProvider.createClient(GithubEnterpriseTests.GITHUB_ENTERPRISE_URL, GITHUB_ENTERPRISE_USER_AGENT);
//        gitHubClient2.setCredentials(FORK_ACCOUNT_NAME, FORK_ACCOUNT_PASSWORD);
//        gitHubTestSupport.addOwner(FORK_ACCOUNT_NAME, gitHubClient2);
//    }
//
//    @Override
//    protected void initLocalTestRepository()
//    {
//        gitHubTestSupport.beforeMethod();
//        gitHubTestSupport.addRepositoryByName(ACCOUNT_NAME, repositoryName,
//                GitHubTestSupport.Lifetime.DURING_TEST_METHOD, EXPIRATION_DURATION_5_MIN);
//
//        dvcs.createTestLocalRepository(ACCOUNT_NAME, repositoryName, ACCOUNT_NAME, PASSWORD);
//    }
//
//    @Override
//    protected void cleanupLocalTestRepository()
//    {
//        // delete account in local configuration first to avoid 404 error when uninstalling hook
//        dvcs.deleteAllRepositories();
//        gitHubTestSupport.afterMethod();
//    }
//
//    @Override
//    protected AccountsPageAccount.AccountType getAccountType()
//    {
//        return AccountsPageAccount.AccountType.GIT_HUB_ENTERPRISE;
//    }
}
