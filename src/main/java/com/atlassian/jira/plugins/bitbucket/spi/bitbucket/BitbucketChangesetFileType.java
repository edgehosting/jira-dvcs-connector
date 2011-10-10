package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;

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
