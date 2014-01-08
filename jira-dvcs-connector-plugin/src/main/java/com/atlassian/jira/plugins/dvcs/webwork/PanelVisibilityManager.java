package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.plugin.PluginAccessor;

public class PanelVisibilityManager
{
    private static final String DEVSUMMARY_PLUGIN_ID = "com.atlassian.jira.plugins.jira-development-integration-plugin";

    private final PermissionManager permissionManager;
    private final PluginAccessor pluginAccessor;

    public PanelVisibilityManager(PermissionManager permissionManager, PluginAccessor pluginAccessor)
    {
        this.permissionManager = permissionManager;
        this.pluginAccessor = pluginAccessor;
    }

    public boolean showPanel(Issue issue, User user)
    {
        return (!pluginAccessor.isPluginEnabled(DEVSUMMARY_PLUGIN_ID) ||
                // JIRA 6.1.x was installed with 0.x of the devsummary plugin, everything else after will want to hide this panel
                pluginAccessor.getPlugin(DEVSUMMARY_PLUGIN_ID).getPluginInformation().getVersion().startsWith("0.")) &&
                permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user);
    }
}
