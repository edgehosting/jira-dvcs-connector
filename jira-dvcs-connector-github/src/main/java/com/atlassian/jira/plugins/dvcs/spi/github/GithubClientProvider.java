package com.atlassian.jira.plugins.dvcs.spi.github;

import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_API;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_DEFAULT;
import static org.eclipse.egit.github.core.client.IGitHubConstants.HOST_GISTS;

import java.io.IOException;
import java.net.URL;

import org.eclipse.egit.github.core.client.GitHubClient;
import org.eclipse.egit.github.core.service.CommitService;
import org.eclipse.egit.github.core.service.RepositoryService;
import org.eclipse.egit.github.core.service.UserService;

import com.atlassian.jira.plugins.dvcs.auth.Authentication;
import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.auth.impl.OAuthAuthentication;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.util.DvcsConstants;
import com.atlassian.plugin.PluginAccessor;

public class GithubClientProvider
{
    private final AuthenticationFactory authenticationFactory;
    private final String userAgent;

    public GithubClientProvider(AuthenticationFactory authenticationFactory, PluginAccessor pluginAccessor)
    {
        this.authenticationFactory = authenticationFactory;
        this.userAgent = DvcsConstants.getUserAgent(pluginAccessor);
    }

    public GitHubClient createClient(Repository repository)
    {
        GitHubClient client = createClientInternal(repository.getOrgHostUrl(), userAgent);
        OAuthAuthentication auth = (OAuthAuthentication) authenticationFactory.getAuthentication(repository);
        client.setOAuth2Token(auth.getAccessToken());
        
        return client;
    }

	public GitHubClient createClient(String hostUrl)
    {
        return createClientInternal(hostUrl, userAgent);
    }
    
    protected GitHubClient createClientInternal(String url, String userAgent)
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
        } else
        {
            throw new SourceControlException("Failed to get proper OAuth instance for github client.");
        }
        return client;
    }
    
    public CommitService getCommitService(Repository repository)
    {
        return new CommitService(createClient(repository));
    }

    public UserService getUserService(Repository repository)
    {
        return new UserService(createClient(repository));
    }

    public RepositoryService getRepositoryService(Repository repository)
    {
        return new RepositoryService(createClient(repository));
    }

    public RepositoryService getRepositoryService(Organization organization)
    {
        return new RepositoryService(createClient(organization));
    }


    /**
     * Create a GitHubClient to connect to the api.
     *
     * It uses the right host in case we're calling the github.com api.
     * It uses the right protocol in case we're calling the GitHub Enterprise api.
     *
     * @param url is the GitHub's oauth host.
     * @param userAgent 
     * @return a GitHubClient
     */
    public static GitHubClient createClient(String url, String userAgent)
    {
        try
        {
            URL urlObject = new URL(url);
            String host = urlObject.getHost();

            if (HOST_DEFAULT.equals(host) || HOST_GISTS.equals(host))
            {
                host = HOST_API;
            }

            GitHubClient result = new GitHubClient(host, -1, urlObject.getProtocol());
            result.setUserAgent(userAgent);
            return result;
        } catch (IOException e)
        {
            throw new IllegalArgumentException(e);
        }
    }
    
}
