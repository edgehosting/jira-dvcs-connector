package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

public class ViewVersionControlCondition implements Condition
{
    private final PanelVisibleManager panelVisibleManager;

    public ViewVersionControlCondition(PanelVisibleManager panelVisibleManager)
    {
        this.panelVisibleManager = panelVisibleManager;
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
        return panelVisibleManager.showPanel(issue, user);
    }
}
