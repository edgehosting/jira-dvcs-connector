package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.List;
import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.githubenterprise.GithubEnterpriseCommunicator;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.ApplicationUsers;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

public class ViewVersionControlCondition implements Condition
{
    private final PermissionManager permissionManager;
    private final FeatureManager featureManager;
    private final OrganizationService organizationService;

    public ViewVersionControlCondition(final PermissionManager permissionManager, final FeatureManager featureManager, final OrganizationService organizationService)
    {
        this.permissionManager = permissionManager;
        this.featureManager = featureManager;
        this.organizationService = organizationService;
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException
    {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> context)
    {
        User user = (User) context.get("user");
        Issue issue = (Issue) context.get("issue");
        ApplicationUser auser = ApplicationUsers.from(user);

        boolean optedIn = featureManager.isEnabledForUser(auser, DvcsTabPanel.LABS_OPT_IN);
        return (permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user)
                && (!optedIn || isGithubConnected()));
    }

    private boolean isGithubConnected()
    {
        return organizationService.existsOrganizationWithType(GithubCommunicator.GITHUB, GithubEnterpriseCommunicator.GITHUB_ENTERPRISE);
    }

}
