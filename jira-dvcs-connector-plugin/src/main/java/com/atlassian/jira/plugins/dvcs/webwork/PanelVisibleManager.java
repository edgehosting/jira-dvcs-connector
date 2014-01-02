package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.plugin.PluginAccessor;

public class PanelVisibleManager
{
    private static final String DEVSUMMARY_PLUGIN_ID = "com.atlassian.jira.plugins.jira-development-integration-plugin";
    private static final String LABS_OPT_IN = "jira.plugin.devstatus.phasetwo";

    private final FeatureManager featureManager;
    private final OrganizationService organizationService;
    private final PermissionManager permissionManager;
    private final PluginAccessor pluginAccessor;

    public PanelVisibleManager(FeatureManager featureManager, OrganizationService organizationService, PermissionManager permissionManager, PluginAccessor pluginAccessor)
    {
        this.featureManager = featureManager;
        this.organizationService = organizationService;
        this.permissionManager = permissionManager;
        this.pluginAccessor = pluginAccessor;
    }

    public boolean showPanel(Issue issue, User user)
    {
        return (!pluginAccessor.isPluginEnabled(DEVSUMMARY_PLUGIN_ID) ||
                // JIRA 6.1.x was installed with 0.x of the devsummary plugin, everything else after will want to hide this panel
                pluginAccessor.getPlugin(DEVSUMMARY_PLUGIN_ID).getPluginInformation().getVersion().startsWith("0.")) &&
                permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user) &&
                (!featureManager.isEnabledForUser(ApplicationUsers.from(user), LABS_OPT_IN) || isGithubConnected());
    }

    private boolean isGithubConnected()
    {
        return organizationService.existsOrganizationWithType(GithubCommunicator.GITHUB, GithubEnterpriseCommunicator.GITHUB_ENTERPRISE);
    }
}
