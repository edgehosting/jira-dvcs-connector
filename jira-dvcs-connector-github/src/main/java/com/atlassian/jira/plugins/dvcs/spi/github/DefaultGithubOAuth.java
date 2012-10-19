package com.atlassian.jira.plugins.dvcs.spi.github;

import org.apache.commons.lang.StringUtils;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class DefaultGithubOAuth implements GithubOAuth
{
    private final PluginSettingsFactory pluginSettingsFactory;

    public DefaultGithubOAuth(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public void setClient(String host, String clientID, String clientSecret)
    {
    	pluginSettingsFactory.createGlobalSettings().put("githubHost", StringUtils.trim(host));
        pluginSettingsFactory.createGlobalSettings().put("githubRepositoryClientID", StringUtils.trim(clientID));
        pluginSettingsFactory.createGlobalSettings().put("githubRepositoryClientSecret", StringUtils.trim(clientSecret));
    }

    @Override
    public String getClientId()
    {
        String savedClientSecret = (String) pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientID");
        return StringUtils.isBlank(savedClientSecret) ? "" : savedClientSecret;
    }

    @Override
    public String getClientSecret()
    {
        String savedClientSecret = (String) pluginSettingsFactory.createGlobalSettings().get("githubRepositoryClientSecret");
        return StringUtils.isBlank(savedClientSecret) ? "" : savedClientSecret;
    }

    @Override
    public String getHost()
    {
        String savedGitHubHost = (String) pluginSettingsFactory.createGlobalSettings().get("githubHost");
        return StringUtils.isBlank(savedGitHubHost) ? "" : savedGitHubHost;
    }

}
