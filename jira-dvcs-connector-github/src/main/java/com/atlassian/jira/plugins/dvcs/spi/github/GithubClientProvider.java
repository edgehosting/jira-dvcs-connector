package com.atlassian.jira.plugins.dvcs.spi.github;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.auth.impl.OAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.EventService;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;

import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_API;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_DEFAULT;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_GISTS;

@Component
public class GithubClientProvider
{
    private final AuthenticationFactory authenticationFactory;
    private final String userAgent;

    @Autowired
    public GithubClientProvider(AuthenticationFactory authenticationFactory, @ComponentImport PluginAccessor pluginAccessor)
    {
        this.authenticationFactory = authenticationFactory;
        this.userAgent = DvcsConstants.getUserAgent(pluginAccessor);
    }

    public GithubClientWithTimeout createClient(Repository repository)
    {
        GithubClientWithTimeout client = createClientInternal(repository.getOrgHostUrl(), userAgent);
        OAuthAuthentication auth = (OAuthAuthentication) authenticationFactory.getAuthentication(repository);
        client.setOAuth2Token(auth.getAccessToken());

        return client;
    }

    public GitHubClient createClient(String hostUrl)
    {
        return createClientInternal(hostUrl, userAgent);
    }

    protected GithubClientWithTimeout createClientInternal(String url, String userAgent)
    {
        return createClient(url, userAgent);
    }

    public GitHubClient createClient(Organization organization)
    {
        GitHubClient client = createClientInternal(organization.getHostUrl(), userAgent);
        Authentication authentication = authenticationFactory.getAuthentication(organization);
        if (authentication instanceof OAuthAuthentication)
        {
            OAuthAuthentication oAuth = (OAuthAuthentication) authentication;
            client.setOAuth2Token(oAuth.getAccessToken());
        }
        else
        {
            throw new SourceControlException("Failed to get proper OAuth instance for github client.");
        }
        return client;
    }

    public CommitService getCommitService(Repository repository)
    {
        return new CommitService(createClient(repository));
    }

    public UserService getUserService(Organization organization)
    {
        return new UserService(createClient(organization));
    }

    public UserService getUserService(Repository repository)
    {
        GithubClientWithTimeout client = createClient(repository);
        client.setTimeout(2000);
        return new UserService(client);
    }

    public RepositoryService getRepositoryService(Repository repository)
    {
        return new RepositoryService(createClient(repository));
    }

    public RepositoryService getRepositoryService(Organization organization)
    {
        return new RepositoryService(createClient(organization));
    }

    public CustomPullRequestService getPullRequestService(Repository repository)
    {
        return new CustomPullRequestService(createClient(repository));
    }

    public IssueService getIssueService(Repository repository)
    {
        return new IssueService(createClient(repository));
    }

    public EventService getEventService(Repository repository)
    {
        return new EventService(createClient(repository));
    }

    /**
     * Create a GithubClientWithTimeout to connect to the api.
     * <p/>
     * It uses the right host in case we're calling the github.com api. It uses the right protocol in case we're calling
     * the GitHub Enterprise api.
     *
     * @param url is the GitHub's oauth host.
     * @return a GithubClientWithTimeout
     */
    public static GithubClientWithTimeout createClient(String url, String userAgent)
    {
        try
        {
            URL urlObject = new URL(url);
            String host = urlObject.getHost();

            if (HOST_DEFAULT.equals(host) || HOST_GISTS.equals(host))
            {
                host = HOST_API;
            }

            GithubClientWithTimeout result = new GithubClientWithTimeout(host, -1, urlObject.getProtocol());
            result.setUserAgent(userAgent);
            return result;
        }
        catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

}
