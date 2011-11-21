package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.ChangesetFile;
import com.atlassian.jira.plugins.bitbucket.api.ChangesetFileAction;

public class DefaultBitbucketChangesetFile implements ChangesetFile
{
    private final ChangesetFileAction type;
    private final String file;

    public DefaultBitbucketChangesetFile(ChangesetFileAction type, String file)
    {
        this.type = type;
        this.file = file;
    }

    public ChangesetFileAction getFileAction()
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
