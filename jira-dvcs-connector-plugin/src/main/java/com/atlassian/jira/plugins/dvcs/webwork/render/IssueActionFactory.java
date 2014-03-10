package com.atlassian.jira.plugins.dvcs.webwork.render;

import com.atlassian.jira.plugin.issuetabpanel.IssueAction;

public interface IssueActionFactory
{
    IssueAction create(Object activityItem);

    Class<? extends Object> getSupportedClass();
}
