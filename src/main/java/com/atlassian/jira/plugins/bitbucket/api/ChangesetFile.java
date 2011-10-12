package com.atlassian.jira.plugins.bitbucket.api;


/**
 * Details about a file that has changed in a {@link Changeset}.
 */
public interface ChangesetFile
{
    public ChangesetFileAction getFileAction();

    public String getFile();
}
