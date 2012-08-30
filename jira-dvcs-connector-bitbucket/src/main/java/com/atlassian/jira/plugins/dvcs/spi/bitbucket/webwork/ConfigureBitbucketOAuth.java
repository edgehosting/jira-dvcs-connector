package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketOAuth;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

public class ConfigureBitbucketOAuth extends JiraWebActionSupport
{
	private static final long serialVersionUID = 4351302596219869689L;

	private final Logger logger = LoggerFactory.getLogger(ConfigureBitbucketOAuth.class);
    private final BitbucketOAuth bitbucketOAuth;
    private final ApplicationProperties applicationProperties;
    
    private String forceClear;

    public ConfigureBitbucketOAuth(@Qualifier("bitbucketOAuth") BitbucketOAuth bitbucketOAuth, ApplicationProperties applicationProperties)
    {
        this.bitbucketOAuth = bitbucketOAuth;
        this.applicationProperties = applicationProperties;
    }

    @Override
    protected void doValidation()
    {
        if (StringUtils.isNotBlank(forceClear))
        {
            clientSecret = "";
            clientID = "";
            return;
        }

        if (StringUtils.isBlank(clientSecret) || StringUtils.isBlank(clientID))
        {
            addErrorMessage("Please enter both the Bitbucket OAuth Key and Secret.");
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
        bitbucketOAuth.setClient(clientID, clientSecret);
        messages = "Bitbucket client credentials set correctly.";
    }

    public String getSavedClientSecret()
    {
        return bitbucketOAuth.getClientSecret();
    }

    public String getSavedClientID()
    {
        return bitbucketOAuth.getClientId();
    }

    // Client ID 
    private String clientID = "";

    public void setClientID(String value)
    {
        clientID = value;
    }

    public String getClientID()
    {
        return clientID;
    }

    // Client Secret 
    private String clientSecret = "";

    public void setClientSecret(String value)
    {
        clientSecret = value;
    }

    public String getClientSecret()
    {
        return clientSecret;
    }

    // Confirmation Messages
    private String messages = "";


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