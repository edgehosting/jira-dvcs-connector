package com.atlassian.jira.plugins.bitbucket.bitbucket.impl;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFile;
import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangesetFileType;

public class DefaultBitbucketChangesetFile implements BitbucketChangesetFile
{
    private final BitbucketChangesetFileType type;
    private final String file;

    public DefaultBitbucketChangesetFile(BitbucketChangesetFileType type, String file)
    {
        this.type = type;
        this.file = file;
    }

    public BitbucketChangesetFileType getType()
    {
        return type;
    }

    public String getFile()
    {
        return file;
    }
}
