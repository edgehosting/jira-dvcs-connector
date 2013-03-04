package com.atlassian.jira.plugins.dvcs.conditions;

import java.util.Map;

import com.atlassian.plugin.PluginParseException;

/**
 * @author Martin Skurla
 */
public class GithubEnterpriseEnabledCondition implements com.atlassian.plugin.web.Condition
{

    @Override
    public void init(Map<String, String> params) throws PluginParseException
    {
    }

    @Override
    public boolean shouldDisplay(Map<String, Object> context)
    {
        return isGitHubEnterpriseEnabled();
    }

    public static boolean isGitHubEnterpriseEnabled()
    {
        return System.getProperty("dvcs.connector.disableGithubEnterprise") == null;
    }
}
