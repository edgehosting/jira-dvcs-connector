package com.atlassian.jira.plugins.dvcs.spi.githubenterprise;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.dvcs.spi.github.GithubOAuth;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class DefaultGithubEnterpriseOAuth implements GithubOAuth
{
    private final PluginSettingsFactory pluginSettingsFactory;

    public DefaultGithubEnterpriseOAuth(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public void setClient(String hostUrl, String clientID, String clientSecret)
    {
        pluginSettingsFactory.createGlobalSettings().put("ghEnterpriseRepositoryClientID", StringUtils.trim(clientID));
        pluginSettingsFactory.createGlobalSettings().put("ghEnterpriseRepositoryClientSecret", StringUtils.trim(clientSecret));
        pluginSettingsFactory.createGlobalSettings().put("ghEnterpriseRepositoryHostUrl", StringUtils.trim(hostUrl));
    }

    @Override
    public String getClientId()
    {
        String savedClientSecret = (String) pluginSettingsFactory.createGlobalSettings().get("ghEnterpriseRepositoryClientID");
        return StringUtils.isBlank(savedClientSecret) ? "" : savedClientSecret;
    }

    @Override
    public String getClientSecret()
    {
        String savedClientSecret = (String) pluginSettingsFactory.createGlobalSettings().get("ghEnterpriseRepositoryClientSecret");
        return StringUtils.isBlank(savedClientSecret) ? "" : savedClientSecret;
    }

    @Override
    public String getHostUrl()
    {
        String savedHostUrl = (String) pluginSettingsFactory.createGlobalSettings().get("ghEnterpriseRepositoryHostUrl");
        return StringUtils.isBlank(savedHostUrl) ? "" : savedHostUrl;
    }
}
