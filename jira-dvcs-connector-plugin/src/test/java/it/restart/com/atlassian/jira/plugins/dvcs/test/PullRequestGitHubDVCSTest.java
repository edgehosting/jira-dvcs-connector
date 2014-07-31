package it.restart.com.atlassian.jira.plugins.dvcs.test;

import it.restart.com.atlassian.jira.plugins.dvcs.RepositoriesPageController;
import it.restart.com.atlassian.jira.plugins.dvcs.common.MagicVisitor;
import it.restart.com.atlassian.jira.plugins.dvcs.common.OAuth;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.page.account.AccountsPageAccount.AccountType;

import org.eclipse.egit.github.core.client.GitHubClient;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestResource;
import com.atlassian.jira.plugins.dvcs.pageobjects.page.OAuthCredentials;
import com.atlassian.pageobjects.TestedProductFactory;

/**
 * Pull request GitHub related tests.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class PullRequestGitHubDVCSTest extends BasePullRequestGitHubDVCSTest
{

    /**
     * @see JiraTestedProduct
     */
    private JiraTestedProduct jiraTestedProduct = TestedProductFactory.create(JiraTestedProduct.class);

    /**
     * @see GitHubTestResource
     */
    private GitHubTestResource gitHubResource = new GitHubTestResource(this, new MagicVisitor(jiraTestedProduct));

    /**
     * {@inheritDoc}
     */
    @Override
    protected void addOrganizations()
    {
        OAuth oAuth = gitHubResource.addOAuth(GitHubTestResource.URL, jiraTestedProduct.getProductInstance().getBaseUrl(),
                GitHubTestResource.Lifetime.DURING_CLASS);

        new MagicVisitor(jiraTestedProduct).visit(GithubLoginPage.class).doLogin();
        OAuthCredentials oAuthCredentials = new OAuthCredentials(oAuth.key, oAuth.secret);
        RepositoriesPageController repositoriesPageController = new RepositoriesPageController(jiraTestedProduct);
        repositoriesPageController.getPage().deleteAllOrganizations();
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.GITHUB, GitHubTestResource.USER,
                oAuthCredentials, false);
        repositoriesPageController.addOrganization(RepositoriesPageController.AccountType.GITHUB, GitHubTestResource.ORGANIZATION,
                oAuthCredentials, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setupGitHubResource(GitHubTestResource gitHubTestResource)
    {
        GitHubClient gitHubClient = GitHubClient.createClient(GitHubTestResource.URL);
        gitHubClient.setCredentials(GitHubTestResource.USER, GitHubTestResource.USER_PASSWORD);
        gitHubTestResource.addOwner(GitHubTestResource.USER, gitHubClient);
        gitHubTestResource.addOwner(GitHubTestResource.ORGANIZATION, gitHubClient);

        GitHubClient gitHubClient2 = GitHubClient.createClient(GitHubTestResource.URL);
        gitHubClient2.setCredentials(GitHubTestResource.OTHER_USER, GitHubTestResource.OTHER_USER_PASSWORD);
        gitHubTestResource.addOwner(GitHubTestResource.OTHER_USER, gitHubClient2);
    }

    @Override
    protected String getGitHubUrl()
    {
        return GitHubTestResource.URL;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected AccountType getAccountType()
    {
        return AccountType.GIT_HUB;
    }

    @Override
    protected void logoutFromGitHub()
    {
        // log out from github
        new MagicVisitor(jiraTestedProduct).visit(GithubLoginPage.class).doLogout();
    }

}
