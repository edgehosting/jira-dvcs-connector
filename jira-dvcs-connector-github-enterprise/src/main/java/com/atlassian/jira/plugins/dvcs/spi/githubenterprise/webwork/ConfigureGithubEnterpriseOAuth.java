package com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.ConfigureGithubOAuth;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;

public class ConfigureGithubEnterpriseOAuth extends ConfigureGithubOAuth
{
    // GitHub Enterprise host url
    private String hostUrl;
    
    private static final long serialVersionUID = 5434744550819376738L;

    public ConfigureGithubEnterpriseOAuth(@Qualifier("githubEnterpriseOAuth") GithubOAuth githubOAuth, ApplicationProperties applicationProperties)
    {
        super(githubOAuth, applicationProperties);
    }
	
    @Override
    protected void doValidation()
    {
        super.doValidation();
        
        if (StringUtils.isNotBlank(forceClear))
        {
            hostUrl = "";
            return;
        }
  
        if (StringUtils.isBlank(hostUrl))
        {
            addErrorMessage("Please enter GitHub Enterprise host url.");
        }
        
        if (!SystemUtils.isValid(hostUrl))
        {
            addErrorMessage("Please provide valid GitHub host URL.");
        }
    }

    @Override
    @RequiresXsrfCheck
    protected String doExecute() throws Exception
    {
        super.doExecute();
        
        if (!getHasErrorMessages())
        {
            addClientIdentifiers();
        }

        return INPUT;
    }

    @Override
    protected void addClientIdentifiers()
    {
        githubOAuth.setClient(StringUtils.trim(hostUrl), StringUtils.trim(clientID), StringUtils.trim(clientSecret));
        messages = "GitHub Host URL And Client Identifiers Set Correctly";
    }

    @Override
    public String getSavedClientSecret()
    {
        return githubOAuth.getClientSecret();
    }

    @Override
    public String getSavedClientID()
    {
        return githubOAuth.getClientId();
    }
    
    public String getHostUrl()
    {
        return githubOAuth.getHostUrl();
    }
    
    public void setHostUrl(String hostUrl)
    {
        this.hostUrl = hostUrl;
    }
}