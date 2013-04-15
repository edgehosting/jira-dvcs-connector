package com.atlassian.jira.plugins.dvcs.auth;

import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;

public class DefaultOAuthStore implements OAuthStore
{
    private final PluginSettingsFactory pluginSettingsFactory;

    public DefaultOAuthStore(PluginSettingsFactory pluginSettingsFactory)
    {
        this.pluginSettingsFactory = pluginSettingsFactory;
    }

    @Override
    public void store(Host host, String clientId, String secret)
    {
        // TODO trim values
        pluginSettingsFactory.createGlobalSettings().put("dvcs.connector." + host.id + ".clientId", clientId);
        pluginSettingsFactory.createGlobalSettings().put("dvcs.connector." + host.id + ".secret", secret);
        pluginSettingsFactory.createGlobalSettings().put("dvcs.connector." + host.id + ".url", host.url);
    }

    @Override
    public String getClientId(String hostId)
    {
        return (String) pluginSettingsFactory.createGlobalSettings().get("dvcs.connector." + hostId + ".clientId");
    }

    @Override
    public String getSecret(String hostId)
    {
        return (String) pluginSettingsFactory.createGlobalSettings().get("dvcs.connector." + hostId + ".secret");
    }

    @Override
    public String getUrl(String hostId)
    {
        return (String) pluginSettingsFactory.createGlobalSettings().get("dvcs.connector." + hostId + ".url");
    }

}
