package com.atlassian.jira.plugins.dvcs.model;


/**
 * Describes the type of file action within a {@link Changeset changeset}.
 */
public enum ChangesetFileAction
{
    ADDED("added", "green"), REMOVED("removed", "red"), MODIFIED("modified", "blue");

    private String action;
    private final String color;

    ChangesetFileAction(String action, String color)
    {
        this.action = action;
        this.color = color;
    }

    public String getColor()
    {
        return color;
    }

    public String getAction()
    {
        return action;
    }
}
