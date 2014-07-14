package com.atlassian.jira.plugins.dvcs.model;

import java.util.Date;

public enum PullRequestStatus
{
    OPEN, DECLINED, MERGED;


    public static PullRequestStatus fromRepositoryPullRequestMapping(String status) {
        return fromBitbucketStatus(status);
    }

    public static PullRequestStatus fromBitbucketStatus(String status)
    {
        PullRequestStatus prStatus = valueOf(status);
        return (prStatus != null ? prStatus : OPEN);
    }

    public static PullRequestStatus fromGithubStatus(String status, Date mergedAt)
    {
        if ("open".equalsIgnoreCase(status))
        {
            return PullRequestStatus.OPEN;
        }

        else if ("closed".equalsIgnoreCase(status))
        {
            return mergedAt != null ? PullRequestStatus.MERGED : PullRequestStatus.DECLINED;
        }

        return null;
    }

    @Override
    public String toString()
    {
        return name();
    }
}
