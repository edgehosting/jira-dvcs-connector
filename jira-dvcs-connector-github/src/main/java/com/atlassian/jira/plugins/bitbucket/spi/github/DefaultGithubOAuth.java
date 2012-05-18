package com.atlassian.jira.plugins.bitbucket.spi.github;

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
    public void setClient(String clientID, String clientSecret)
    {
        pluginSettingsFactory.createGlobalSettings().put("githubRepositoryClientID", clientID);
        pluginSettingsFactory.createGlobalSettings().put("githubRepositoryClientSecret", clientSecret);
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

}
