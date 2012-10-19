package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import static com.atlassian.jira.util.UrlValidator.isValid;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.util.UrlValidator;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

public class ConfigureGithubOAuth extends JiraWebActionSupport
{
	private static final long serialVersionUID = 1L;

	final Logger logger = LoggerFactory.getLogger(ConfigureGithubOAuth.class);
   
	private final GithubOAuth githubOAuth;
    private final ApplicationProperties applicationProperties;
    
    // GitHub host, it might be a custom host if it's GitHub Enterprise.
    private String host = "https://github.com";

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
        if (StringUtils.isNotBlank(forceClear))
        {
        	host = "";
            clientSecret = "";
            clientID = "";
            return;
        }

        if (StringUtils.isBlank(host))
        {
        	host = "https://github.com";
        }
        else if (!isValid(host))
        {
    		addErrorMessage("Please provide a valid url for the GitHub host");
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
        githubOAuth.setClient(StringUtils.trim(host), StringUtils.trim(clientID), StringUtils.trim(clientSecret));
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

    public String getSavedHost()
    {
    	return githubOAuth.getHost();
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
	
	public String getHost()
	{
		return host;
	}

	public void setHost(String host)
	{
		this.host = host;
	}

}