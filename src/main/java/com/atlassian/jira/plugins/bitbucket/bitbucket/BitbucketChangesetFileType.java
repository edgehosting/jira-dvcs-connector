package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.common.Changeset;

/**
 * Describes the type of file action within a {@link Changeset changeset}.
 */
public enum BitbucketChangesetFileType
{
    ADDED("green"), REMOVED("red"), MODIFIED("blue");

    private final String color;

    BitbucketChangesetFileType(String color)
    {
        this.color = color;
    }

    public String getColor()
    {
        return color;
    }
}
