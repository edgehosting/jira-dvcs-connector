package com.atlassian.jira.plugins.dvcs.model;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

public class ChangesetFile
{
    private final ChangesetFileAction type;
    private final String file;
    private final int additions;
    private final int deletions;

    public ChangesetFile(ChangesetFileAction type, String file, int additions, int deletions)
    {
        this.type = type;
        this.file = file;
        this.additions = additions;
        this.deletions = deletions;
    }

    public ChangesetFileAction getFileAction()
    {
        return type;
    }

    public String getFile()
    {
        return file;
    }

    public int getAdditions()
    {
        return additions;
    }

    public int getDeletions()
    {
        return deletions;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ChangesetFile that = (ChangesetFile) o;

        return new EqualsBuilder()
                .append(type, that.type)
                .append(file, that.file)
                .append(additions, that.additions)
                .append(deletions, that.deletions)
                .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder()
                .append(type)
                .append(file)
                .append(additions)
                .append(deletions)
                .hashCode();
    }
}

