package com.atlassian.jira.plugins.bitbucket.spi.github.webwork;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.spi.github.GithubOAuth;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

public class ConfigureGithubOAuth extends JiraWebActionSupport
{
    final Logger logger = LoggerFactory.getLogger(ConfigureGithubOAuth.class);
    private final GithubOAuth githubOAuth;
    private final ApplicationProperties applicationProperties;

    public ConfigureGithubOAuth(GithubOAuth githubOAuth, ApplicationProperties applicationProperties)
    {
        this.githubOAuth = githubOAuth;
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void doValidation()
    {
        if (StringUtils.isBlank(clientSecret) || StringUtils.isBlank(clientID))
        {
            addErrorMessage("Please enter both the GitHub OAuth Client ID and Client Secret.");
        }
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        if (!getHasErrorMessages())
        {
            addClientIdentifiers();
        }

        return INPUT;
    }

    private void addClientIdentifiers()
    {
        githubOAuth.setClient(clientID, clientSecret);
        messages = "GitHub Client Identifiers Set Correctly";
    }

    public String getSavedClientSecret()
    {
        return githubOAuth.getClientSecret();
    }

    public String getSavedClientID()
    {
        return githubOAuth.getClientId();
    }

    // Client ID (from GitHub OAuth Application)
    private String clientID = "";

    public void setClientID(String value)
    {
        this.clientID = value;
    }

    public String getClientID()
    {
        return this.clientID;
    }

    // Client Secret (from GitHub OAuth Application)
    private String clientSecret = "";

    public void setClientSecret(String value)
    {
        this.clientSecret = value;
    }

    public String getClientSecret()
    {
        return this.clientSecret;
    }

    // Confirmation Messages
    private String messages = "";

    public String getMessages()
    {
        return this.messages;
    }
    
    public String getBaseUrl()
    {
        return applicationProperties.getBaseUrl();
    }

}