package com.atlassian.jira.plugins.dvcs.spi.github;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class DefaultGithubOAuth implements GithubOAuth
{
    private final PluginSettingsFactory pluginSettingsFactory;
    private static final Logger log = LoggerFactory.getLogger(DefaultGithubOAuth.class);

    public DefaultGithubOAuth(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public void setClient(String hostUrl, String clientID, String clientSecret)
    {
        if (hostUrl!=null)
        {
            log.error("host Url should be null for github.com");
        }
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
    public String getHostUrl()
    {
        return null;
    }
}
