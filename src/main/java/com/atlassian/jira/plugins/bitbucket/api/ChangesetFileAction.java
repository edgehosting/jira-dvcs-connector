package com.atlassian.jira.plugins.bitbucket.api;


/**
 * Describes the type of file action within a {@link Changeset changeset}.
 */
public enum ChangesetFileAction
{
    ADDED("green"), REMOVED("red"), MODIFIED("blue");

    private final String color;

    ChangesetFileAction(String color)
    {
        this.color = color;
    }

    public String getColor()
    {
        return color;
    }
}
