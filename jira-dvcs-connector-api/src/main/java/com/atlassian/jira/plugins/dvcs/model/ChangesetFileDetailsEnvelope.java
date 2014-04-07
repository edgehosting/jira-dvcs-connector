package com.atlassian.jira.plugins.dvcs.model;

import java.util.List;

/**
 * Changeset file details file information with first few details and number of changes
 *
 * @since 2.0.3, 2.1.3
 */
public class ChangesetFileDetailsEnvelope
{
    private final List<ChangesetFileDetail> fileDetails;
    private final int count;

    public ChangesetFileDetailsEnvelope(final List<ChangesetFileDetail> fileDetails, final int count)
    {
        this.fileDetails = fileDetails;
        this.count = count;
    }

    public List<ChangesetFileDetail> getFileDetails()
    {
        return fileDetails;
    }

    public int getCount()
    {
        return count;
    }
}
