package com.atlassian.jira.plugins.dvcs.model;

import java.util.List;

/**
 * Changeset file details file information with first few details and number of changes
 */
public class ChangesetFileDetailsEnvelope
{
    private List<ChangesetFileDetail> fileDetails;
    private int count;

    public ChangesetFileDetailsEnvelope(final List<ChangesetFileDetail> fileDetails, final int count)
    {
        this.fileDetails = fileDetails;
        this.count = count;
    }

    public List<ChangesetFileDetail> getFileDetails()
    {
        return fileDetails;
    }

    public void setFileDetails(final List<ChangesetFileDetail> fileDetails)
    {
        this.fileDetails = fileDetails;
    }

    public int getCount()
    {
        return count;
    }

    public void setCount(final int count)
    {
        this.count = count;
    }
}
