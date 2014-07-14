package com.atlassian.jira.plugins.dvcs.model;

public enum PullRequestStatus
{
    OPEN, DECLINED, MERGED;

    public static PullRequestStatus from(String status)
    {
        PullRequestStatus prStatus = valueOf(status);
        return (prStatus != null ? prStatus : OPEN);
    }

    @Override
    public String toString()
    {
        return name();
    }
}
