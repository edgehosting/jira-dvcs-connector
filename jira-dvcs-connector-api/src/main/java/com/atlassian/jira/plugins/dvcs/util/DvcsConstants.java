package com.atlassian.jira.plugins.dvcs.util;

import com.atlassian.plugin.PluginAccessor;

public class DvcsConstants
{
    public static final String LINKERS_ENABLED_SETTINGS_PARAM = "dvcs.BITBUCKET_LINKERS_ENABLED";
    public static final String SOY_TEMPLATE_PLUGIN_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin:soy-templates-plugin-context";

    /**
     * The unique key identifying this plugin to JIRA.
     */
    public static final String PLUGIN_KEY = "com.atlassian.jira.plugins.jira-bitbucket-connector-plugin";

    public static String getPluginVersion(PluginAccessor pluginAccessor)
    {
        return pluginAccessor.getPlugin(PLUGIN_KEY).getPluginInformation().getVersion();
    }
    
    public static String getUserAgent(PluginAccessor pluginAccessor)
    {
        return "JIRA DVCS Connector/"+ getPluginVersion(pluginAccessor);
    }
}
