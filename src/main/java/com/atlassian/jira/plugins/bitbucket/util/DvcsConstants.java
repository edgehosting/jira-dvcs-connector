package com.atlassian.jira.plugins.bitbucket.util;

import com.atlassian.plugin.PluginAccessor;

public class DvcsConstants
{
    private static final String PLUGIN_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin";

    public static String getPluginVersion(PluginAccessor pluginAccessor)
    {
        return pluginAccessor.getPlugin(PLUGIN_KEY).getPluginInformation().getVersion();
    }
    
    public static String getUserAgent(PluginAccessor pluginAccessor)
    {
        return "JIRA DVCS Connector/"+ getPluginVersion(pluginAccessor);
    }
}
