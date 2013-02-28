package com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork;

import static com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator.GITHUB_ENTERPRISE;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.GithubOAuthUtils;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.RegenerateGithubOauthToken;
import com.atlassian.sal.api.ApplicationProperties;

public class RegenerateGithubEnterpriseOauthToken extends RegenerateGithubOauthToken
{
    public RegenerateGithubEnterpriseOauthToken(OrganizationService organizationService, OAuthStore oAuthStore,
            ApplicationProperties applicationProperties)
    {
        super(organizationService, applicationProperties, oAuthStore);
    }
	
    @Override
    protected GithubOAuthUtils getOAuthUtils()
    {
        return new GithubOAuthUtils(baseUrl, oAuthStore.getClientId(GITHUB_ENTERPRISE), oAuthStore.getSecret(GITHUB_ENTERPRISE));
    }

    @Override
    protected String getRedirectAction()
    {
        return "RegenerateGithubEnterpriseOauthToken"; 
    }
}
