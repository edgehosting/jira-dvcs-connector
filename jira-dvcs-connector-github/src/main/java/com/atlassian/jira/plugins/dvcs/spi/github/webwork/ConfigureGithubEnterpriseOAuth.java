package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.sal.api.ApplicationProperties;

public class ConfigureGithubEnterpriseOAuth extends ConfigureGithubOAuth
{

    private static final long serialVersionUID = 5434744550819376738L;

    public ConfigureGithubEnterpriseOAuth(GithubOAuth githubOAuth, ApplicationProperties applicationProperties)
    {
        super(githubOAuth, applicationProperties);
    }
	
    protected void addClientIdentifiers()
    {
        githubOAuth.setEnterpriseClient(StringUtils.trim(clientID), StringUtils.trim(clientSecret));
        messages = "GitHub Client Identifiers Set Correctly";
    }

    public String getSavedClientSecret()
    {
        return githubOAuth.getEnterpriseClientSecret();
    }

    public String getSavedClientID()
    {
        return githubOAuth.getEnterpriseClientId();
    }
}