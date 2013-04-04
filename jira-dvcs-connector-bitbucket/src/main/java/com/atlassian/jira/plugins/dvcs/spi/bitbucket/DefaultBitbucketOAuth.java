package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import org.apache.commons.lang.StringUtils;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class DefaultBitbucketOAuth implements BitbucketOAuth
{
    private final PluginSettingsFactory pluginSettingsFactory;

    public DefaultBitbucketOAuth(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public void setClient(String clientID, String clientSecret)
    {
        pluginSettingsFactory.createGlobalSettings().put("bitbucketRepositoryClientID", trim(clientID));
        pluginSettingsFactory.createGlobalSettings().put("bitbucketRepositoryClientSecret", trim(clientSecret));
    }

    private String trim(String string)
    {
        return string != null ? string.trim() : string;
    }

    @Override
    public String getClientId()
    {
        String savedClientSecret = (String) pluginSettingsFactory.createGlobalSettings().get("bitbucketRepositoryClientID");
        return StringUtils.isBlank(savedClientSecret) ? "" : savedClientSecret;
    }

    @Override
    public String getClientSecret()
    {
        String savedClientSecret = (String) pluginSettingsFactory.createGlobalSettings().get("bitbucketRepositoryClientSecret");
        return StringUtils.isBlank(savedClientSecret) ? "" : savedClientSecret;
    }

}