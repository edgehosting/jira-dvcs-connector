package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.DefaultGithubOauthProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.sal.api.ApplicationProperties;

public class RegenerateGithubEnterpriseOauthToken extends RegenerateGithubOauthToken
{
    private static final long serialVersionUID = 8618932469969976783L;

	public RegenerateGithubEnterpriseOauthToken(OrganizationService organizationService, GithubOAuth githubOAuth,
            GithubOAuthUtils githubOAuthUtils,
            ApplicationProperties applicationProperties)
	{
	    super(organizationService,  new GithubOAuthUtils(
	               DefaultGithubOauthProvider.createEnterpriseProvider(githubOAuth), 
	               githubOAuth,
	               applicationProperties));
	}
	
    protected String getRedirectAction()
    {
        return "RegenerateGithubEnterpriseOauthToken"; 
    }
}
