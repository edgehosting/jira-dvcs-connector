package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

public class DvcsPanelCondition implements Condition
{
    private final PanelVisibilityManager panelVisibilityManager;

    public DvcsPanelCondition(PanelVisibilityManager panelVisibilityManager)
    {
        this.panelVisibilityManager = panelVisibilityManager;
    }

    @Override
    public void init(Map<String, String> params) throws PluginParseException
    {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> context)
    {
        // JIRA does this cast inside com.atlassian.jira.plugin.webfragment.conditions.AbstractWebCondition so I assume this is safe
        ApplicationUser user = (ApplicationUser) context.get("user");
        Issue issue = (Issue) context.get("issue");
        return panelVisibilityManager.showPanel(issue, user);
    }
}
