package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.permission.ProjectPermissions;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.google.common.base.Preconditions.checkNotNull;

@Component
public class PanelVisibilityManager
{
    private static final String DEVSUMMARY_PLUGIN_ID = "com.atlassian.jira.plugins.jira-development-integration-plugin";
    private static final String LABS_OPT_IN = "jira.plugin.devstatus.phasetwo";

    private final PermissionManager permissionManager;
    private final PluginAccessor pluginAccessor;
    private final FeatureManager featureManager;

    @Autowired
    @SuppressWarnings("SpringJavaAutowiringInspection")
    public PanelVisibilityManager(@ComponentImport PermissionManager permissionManager,
            @ComponentImport PluginAccessor pluginAccessor,
            @ComponentImport FeatureManager featureManager)
    {
        this.permissionManager = checkNotNull(permissionManager);
        this.pluginAccessor = checkNotNull(pluginAccessor);
        this.featureManager = checkNotNull(featureManager);
    }

    public boolean showPanel(Issue issue, ApplicationUser user)
    {
        return (!pluginAccessor.isPluginEnabled(DEVSUMMARY_PLUGIN_ID) || !featureManager.isEnabled(LABS_OPT_IN) ||
                // JIRA 6.1.x was installed with 0.x of the devsummary plugin, everything else after will want to hide this panel
                pluginAccessor.getPlugin(DEVSUMMARY_PLUGIN_ID).getPluginInformation().getVersion().startsWith("0.")) &&
                permissionManager.hasPermission(ProjectPermissions.VIEW_DEV_TOOLS, issue, user);
    }
}
