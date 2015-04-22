package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.jira.compatibility.util.ApplicationUserUtil;
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
        ApplicationUser user = ApplicationUserUtil.from(context.get("user"));
        Issue issue = (Issue) context.get("issue");
        return panelVisibilityManager.showPanel(issue, user);
    }
}
