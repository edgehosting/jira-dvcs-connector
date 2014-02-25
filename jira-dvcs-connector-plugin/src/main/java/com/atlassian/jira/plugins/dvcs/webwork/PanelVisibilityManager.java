package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.plugin.PluginAccessor;

public class PanelVisibilityManager
{
    private static final String DEVSUMMARY_PLUGIN_ID = "com.atlassian.jira.plugins.jira-development-integration-plugin";
    private static final String LABS_OPT_IN = "jira.plugin.devstatus.phasetwo";

    private final PermissionManager permissionManager;
    private final PluginAccessor pluginAccessor;
    private final FeatureManager featureManager;

    public PanelVisibilityManager(PermissionManager permissionManager, PluginAccessor pluginAccessor, FeatureManager featureManager)
    {
        this.permissionManager = permissionManager;
        this.pluginAccessor = pluginAccessor;
        this.featureManager = featureManager;
    }

    public boolean showPanel(Issue issue, User user)
    {
        return (!pluginAccessor.isPluginEnabled(DEVSUMMARY_PLUGIN_ID) || !featureManager.isEnabled(LABS_OPT_IN) ||
                // JIRA 6.1.x was installed with 0.x of the devsummary plugin, everything else after will want to hide this panel
                pluginAccessor.getPlugin(DEVSUMMARY_PLUGIN_ID).getPluginInformation().getVersion().startsWith("0.")) &&
                permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user);
    }
}
