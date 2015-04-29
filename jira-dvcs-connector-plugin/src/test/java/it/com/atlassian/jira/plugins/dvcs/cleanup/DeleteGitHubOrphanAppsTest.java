package it.com.atlassian.jira.plugins.dvcs.cleanup;

import com.atlassian.jira.plugins.dvcs.pageobjects.common.MagicVisitor;
import com.beust.jcommander.internal.Lists;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubLoginPage;
import it.restart.com.atlassian.jira.plugins.dvcs.github.GithubOAuthApplicationPage;
import org.eclipse.egit.github.core.Authorization;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.OAuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Delete the orphan OAuth Applications created by Webdriver tests for Github.
 */
public class DeleteGitHubOrphanAppsTest extends DeleteOrphanAppsBaseTest
{
    private static final Logger log = LoggerFactory.getLogger(DeleteGitHubOrphanAppsTest.class);

    private static final String GITHUB_URL = "api.github.com";
    private static final String USER_AGENT = "DVCS Connector Test/X.x";

    @Override
    protected void deleteOrphanOAuthApplications(final String repoOwner, final String repoPassword) throws IOException
    {
            final GithubOAuthApplicationPage applicationPage = getGithubOAuthApplicationPage();

            List<Authorization> expiredConsumers = findExpiredConsumers(repoOwner, repoPassword);

            for (Authorization consumer : expiredConsumers)
            {
                applicationPage.removeConsumerForAppName(consumer.getApp().getName());
                log.debug("Consumer deleted: " + consumer.getApp().getName());
            }
    }

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

    private OAuthService createOAuthServiceRest(final String repoOwner, final String repoPassword)
    {
        GitHubClient gitHubClient = new GitHubClient(GITHUB_URL);
        gitHubClient.setUserAgent(USER_AGENT);
        gitHubClient.setCredentials(repoOwner, repoPassword);
        return new OAuthService(gitHubClient);
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

    private GithubOAuthApplicationPage getGithubOAuthApplicationPage()
    {
        return new MagicVisitor(jira).visit(GithubOAuthApplicationPage.class);
    }
}
