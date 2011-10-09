package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.common.Changeset;

/**
 * Details about a file that has changed in a {@link Changeset}.
 */
public interface BitbucketChangesetFile
{
    public BitbucketChangesetFileType getType();

    public String getFile();
}
