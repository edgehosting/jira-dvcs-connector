package com.atlassian.jira.plugins.dvcs.spi.bitbucket.webwork;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.config.CoreFeatures;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore.Host;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.JiraWebActionSupport;
import com.atlassian.sal.api.ApplicationProperties;

public class ConfigureBitbucketOAuth extends JiraWebActionSupport
{
	private final Logger log = LoggerFactory.getLogger(ConfigureBitbucketOAuth.class);
    private String forceClear;
    private final OAuthStore oAuthStore;
    private final ApplicationProperties applicationProperties;
    private final FeatureManager featureManager;

    public ConfigureBitbucketOAuth(OAuthStore oAuthStore,
            ApplicationProperties applicationProperties, FeatureManager featureManager)
    {
        this.oAuthStore = oAuthStore;
        this.applicationProperties = applicationProperties;
        this.featureManager = featureManager;
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
        oAuthStore.store(Host.BITBUCKET, clientID, clientSecret);
        messages = "Bitbucket client credentials set correctly.";
    }

    public String getSavedClientID()
    {
        return oAuthStore.getClientId(Host.BITBUCKET.id);
    }
    
    public String getSavedClientSecret()
    {
        return oAuthStore.getSecret(Host.BITBUCKET.id);
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
    
    public boolean isOnDemandLicense()
    {
        return featureManager.isEnabled(CoreFeatures.ON_DEMAND);
    }

}