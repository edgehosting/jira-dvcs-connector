package com.atlassian.jira.plugins.dvcs.webwork;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.issue.Issue;
import com.atlassian.plugin.PluginParseException;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.web.Condition;

import java.util.Map;

@Scanned
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
        User user = (User) context.get("user");
        Issue issue = (Issue) context.get("issue");
        return panelVisibilityManager.showPanel(issue, user);
    }
}
