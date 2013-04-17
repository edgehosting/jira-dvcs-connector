package com.atlassian.jira.plugins.dvcs.spi.githubenterprise.webwork;

import static com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator.GITHUB_ENTERPRISE;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.plugins.dvcs.spi.github.webwork.ConfigureGithubOAuth;
import com.atlassian.jira.plugins.dvcs.util.SystemUtils;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;

public class ConfigureGithubEnterpriseOAuth extends ConfigureGithubOAuth
{
    // GitHub Enterprise host url
    private String hostUrl;
    
    public ConfigureGithubEnterpriseOAuth(OAuthStore oAuthStore, ApplicationProperties applicationProperties)
    {
        super(oAuthStore, applicationProperties);
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
        oAuthStore.store(new Host(GITHUB_ENTERPRISE, hostUrl), clientID, clientSecret);
        messages = "GitHub Host URL And Client Identifiers Set Correctly";
    }

    @Override
    public String getSavedClientID()
    {
        return oAuthStore.getClientId(GITHUB_ENTERPRISE);
    }

    @Override
    public String getSavedClientSecret()
    {
        return oAuthStore.getSecret(GITHUB_ENTERPRISE);
    }
    
    public String getHostUrl()
    {
        return oAuthStore.getUrl(GITHUB_ENTERPRISE);
    }
    
    public void setHostUrl(String hostUrl)
    {
        this.hostUrl = hostUrl;
    }
}