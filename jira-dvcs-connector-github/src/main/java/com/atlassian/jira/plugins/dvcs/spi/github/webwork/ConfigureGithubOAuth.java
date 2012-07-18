package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

public class ConfigureGithubOAuth extends JiraWebActionSupport
{
	private static final long serialVersionUID = 1L;

	final Logger logger = LoggerFactory.getLogger(ConfigureGithubOAuth.class);
   
	private final GithubOAuth githubOAuth;
    private final ApplicationProperties applicationProperties;
    
    // Client ID (from GitHub OAuth Application)
    private String clientID = "";
    // Client Secret (from GitHub OAuth Application)
    private String clientSecret = "";
    // Confirmation Messages
    private String messages = "";
    
    private String forceClear = "";


    public ConfigureGithubOAuth(GithubOAuth githubOAuth, ApplicationProperties applicationProperties)
    {
        this.githubOAuth = githubOAuth;
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void doValidation()
    {
    	if (StringUtils.isNotBlank(forceClear)) {
    		return;
    	}
  
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
        githubOAuth.setClient(StringUtils.trim(clientID), StringUtils.trim(clientSecret));
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

    public void setClientID(String value)
    {
        clientID = value;
    }

    public String getClientID()
    {
        return clientID;
    }


    public void setClientSecret(String value)
    {
        clientSecret = value;
    }

    public String getClientSecret()
    {
        return clientSecret;
    }


    public String getMessages()
    {
        return messages;
    }
    
    public String getBaseUrl()
    {
        return applicationProperties.getBaseUrl();
    }

	public String getForceClear()
	{
		return forceClear;
	}

	public void setForceClear(String forceClear)
	{
		this.forceClear = forceClear;
	}

}