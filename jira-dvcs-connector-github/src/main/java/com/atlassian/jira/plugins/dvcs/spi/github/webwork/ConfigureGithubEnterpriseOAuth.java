package com.atlassian.jira.plugins.dvcs.spi.github.webwork;

import static com.atlassian.jira.util.UrlValidator.isValid;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.sal.api.ApplicationProperties;

public class ConfigureGithubEnterpriseOAuth extends ConfigureGithubOAuth
{
    // GitHub Enterprise host url
    private String hostUrl;
    
    private static final long serialVersionUID = 5434744550819376738L;

    public ConfigureGithubEnterpriseOAuth(GithubOAuth githubOAuth, ApplicationProperties applicationProperties)
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
        
        if (!isValid(hostUrl))
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

    protected void addClientIdentifiers()
    {
        githubOAuth.setEnterpriseClient(StringUtils.trim(hostUrl), StringUtils.trim(clientID), StringUtils.trim(clientSecret));
        messages = "GitHub Host URL And Client Identifiers Set Correctly";
    }

    public String getSavedClientSecret()
    {
        return githubOAuth.getEnterpriseClientSecret();
    }

    public String getSavedClientID()
    {
        return githubOAuth.getEnterpriseClientId();
    }
    
    public String getHostUrl()
    {
        return githubOAuth.getEnterpriseHostUrl();
    }
    
    public void setHostUrl(String hostUrl)
    {
        this.hostUrl = hostUrl;
    }
}