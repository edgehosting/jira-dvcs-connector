package com.atlassian.jira.plugins.dvcs.smartcommits;

import java.util.List;

/**
 * A parsed commit comment command
 */
public class CommitCommentCommand
{
    private final List<String> issues;
    private final String action;
    private final String arguments;

    public CommitCommentCommand(List<String> issues, String action, String arguments)
    {
        this.issues = issues;
        this.action = action;
        this.arguments = arguments;
    }

    public List<String> getIssues()
    {
        return issues;
    }

    public String getAction()
    {
        return action;
    }

    public String getArguments()
    {
        return arguments;
    }
}
