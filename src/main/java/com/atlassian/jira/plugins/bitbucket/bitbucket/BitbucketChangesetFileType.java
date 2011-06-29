package com.atlassian.jira.plugins.bitbucket.bitbucket;

/**
 * Describes the type of file action within a {@link BitbucketChangeset changeset}.
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
