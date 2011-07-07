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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        DefaultBitbucketChangesetFile that = (DefaultBitbucketChangesetFile) o;

        if (!file.equals(that.file)) return false;
        if (type != that.type) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = type.hashCode();
        result = 31 * result + file.hashCode();
        return result;
    }
}
