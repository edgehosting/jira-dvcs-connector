package com.atlassian.jira.plugins.dvcs.spi.github;

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

public class GithubClientProvider
{
    private final AuthenticationFactory authenticationFactory;

    public GithubClientProvider(AuthenticationFactory authenticationFactory)
    {
        this.authenticationFactory = authenticationFactory;
    }

    private GitHubClient createClient(Repository repository)
    {
        GitHubClient client = GitHubClient.createClient(repository.getOrgHostUrl());
        
        OAuthAuthentication auth = (OAuthAuthentication) authenticationFactory.getAuthentication(repository);
        client.setOAuth2Token(auth.getAccessToken());
        
        return client;
    }
    
    private GitHubClient createClient(Organization organization)
    {
        GitHubClient client = GitHubClient.createClient(organization.getHostUrl());

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

}
