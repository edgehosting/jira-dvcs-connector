package com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork;

import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.GithubOAuthUtils;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.RegenerateGithubOauthToken;
import com.atlassian.sal.api.ApplicationProperties;

public class RegenerateGithubEnterpriseOauthToken extends RegenerateGithubOauthToken
{
    public RegenerateGithubEnterpriseOauthToken(OrganizationService organizationService,
            @Qualifier("githubEnterpriseOAuth") GithubOAuth githubOAuth,
            ApplicationProperties applicationProperties)
	{
        super(organizationService, new GithubOAuthUtils(githubOAuth, applicationProperties));
	}
	
    @Override
    protected String getRedirectAction()
    {
        return "RegenerateGithubEnterpriseOauthToken"; 
    }
}
