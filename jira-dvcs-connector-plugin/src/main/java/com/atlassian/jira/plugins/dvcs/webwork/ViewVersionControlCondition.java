package com.atlassian.jira.plugins.dvcs.webwork;

import java.util.Map;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.plugin.webfragment.JiraWebInterfaceManager;
import com.atlassian.jira.security.PermissionManager;
import com.atlassian.jira.security.Permissions;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

public class ViewVersionControlCondition implements Condition
{
    private final PermissionManager permissionManager;

    public ViewVersionControlCondition(PermissionManager permissionManager)
    {
        this.permissionManager = permissionManager;
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException
    {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> context)
    {
        User user = (User) context.get(JiraWebInterfaceManager.CONTEXT_KEY_USER);
        Issue issue = (Issue) context.get("issue");
        return permissionManager.hasPermission(Permissions.VIEW_VERSION_CONTROL, issue, user);
    }

}
