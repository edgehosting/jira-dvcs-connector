package com.atlassian.jira.plugins.dvcs.model;

public enum PullRequestStatusGitHub
{
    OPEN, CLOSED;

    public static PullRequestStatusGitHub from(String status)
    {
        PullRequestStatusGitHub prStatus = valueOf(status);
        return (prStatus != null ? prStatus : OPEN);
    }

    @Override
    public String toString()
    {
        return name();
    }
}
