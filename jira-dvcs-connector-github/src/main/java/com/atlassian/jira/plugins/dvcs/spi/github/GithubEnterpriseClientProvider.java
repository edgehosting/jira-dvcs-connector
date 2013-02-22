package com.atlassian.jira.plugins.dvcs.spi.github;

import org.eclipse.egit.github.core.client.GitHubClient;

import com.atlassian.jira.plugins.dvcs.auth.AuthenticationFactory;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.GithubOAuthUtils;

public class GithubEnterpriseClientProvider extends GithubClientProvider
{
    public GithubEnterpriseClientProvider(AuthenticationFactory authenticationFactory)
    {
        super(authenticationFactory);
    }

    @Override
    protected GitHubClient createClient(String url)
    {
    	return GithubOAuthUtils.createClientForGithubEnteprise(url);
    }

}
