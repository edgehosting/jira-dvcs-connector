package it.com.atlassian.jira.plugins.dvcs.cleanup;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.atlassian.pageobjects.elements.PageElement;
import com.beust.jcommander.internal.Lists;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthApplicationPage;
import org.eclipse.egit.github.core.Authorization;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.OAuthService;

import java.io.IOException;
import java.util.List;

import static com.atlassian.jira.plugins.dvcs.pageobjects.common.OAuthUtils.TEST_OAUTH_PREFIX;

/**
 * Delete the orphan OAuth Applications created by Webdriver tests for Github.
 */
public class DeleteGitHubOrphanAppsTest extends DeleteOrphanAppsBaseTest
{
    private static final String GITHUB_URL = "api.github.com";
    private static final String USER_AGENT = "DVCS Connector Test/X.x";

    @Override
    protected void login(final String repoOwner, final String repoPassword)
    {
        new MagicVisitor(jira).visit(GithubLoginPage.class).doLogin(repoOwner, repoPassword);
    }

    @Override
    protected void logout()
    {
        new MagicVisitor(jira).visit(GithubLoginPage.class).doLogout();
    }

    @Override
    protected void deleteOrphanOAuthApplications(final String repoOwner, final String repoPassword) throws IOException
    {
        try
        {
            List<Authorization> expiredConsumers = findExpiredConsumers(repoOwner, repoPassword);

            for (Authorization consumer : expiredConsumers)
            {
                final GithubOAuthApplicationPage applicationPage = goToGithubOAuthApplicationPage();
                applicationPage.removeConsumerForAppName(consumer.getApp().getName());
            }
        }
        finally
        {
            // delete the non-authorized Applications filtering them in the UI
            deleteGitHubNonAuthorizedApplications();
        }
    }

    private void deleteGitHubNonAuthorizedApplications()
    {
        GithubOAuthApplicationPage page = goToGithubOAuthApplicationPage();
        List<String> expiredApps = filterExpiredOAuthApplicationsUI(page);

        for (String application : expiredApps)
        {
            page.removeConsumerForAppName(application);
            page = goToGithubOAuthApplicationPage(); // refresh page
        }
    }

    private List<String> filterExpiredOAuthApplicationsUI(GithubOAuthApplicationPage appPage)
    {
        List<String> expiredApps = Lists.newArrayList();
        List<PageElement> applications = appPage.findOAthApplications(TEST_OAUTH_PREFIX);

        for (PageElement link : applications)
        {
            if (super.isConsumerExpired(link.getText()))
            {
                expiredApps.add(link.getText().trim());
            }
        }
        return expiredApps;
    }

    private List<Authorization> findExpiredConsumers(final String repoOwner, final String repoPassword)
            throws IOException
    {
        OAuthService oAuthServiceRest = createOAuthServiceRest(repoOwner, repoPassword);
        List<Authorization> expiredConsumers = Lists.newArrayList();

        // Ideally we should retrieve the Github Applications but no available REST service for that purpose at the moment.
        // There is a small risk of having Applications without Authorizations which are going to be missed in the current
        // logic but acceptable to keep code simple. When Github makes the Applications available via REST then we can update
        // our code.
        List<Authorization> authorizations = oAuthServiceRest.getAuthorizations();

        for (Authorization authorization : authorizations)
        {
            if (super.isConsumerExpired(authorization.getApp().getName()))
            {
                expiredConsumers.add(authorization);
            }
        }
        return expiredConsumers;
    }

    private OAuthService createOAuthServiceRest(final String repoOwner, final String repoPassword)
    {
        GitHubClient gitHubClient = new GitHubClient(GITHUB_URL);
        gitHubClient.setUserAgent(USER_AGENT);
        gitHubClient.setCredentials(repoOwner, repoPassword);
        return new OAuthService(gitHubClient);
    }

    private GithubOAuthApplicationPage goToGithubOAuthApplicationPage()
    {
        return new MagicVisitor(jira).visit(GithubOAuthApplicationPage.class);
    }
}
