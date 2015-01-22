package com.atlassian.jira.plugins.dvcs.model;

import com.google.common.collect.ImmutableList;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Date;

import static org.fest.assertions.api.Assertions.assertThat;

public class ChangesetTest
{
    private Changeset changeset;

    @BeforeMethod
    public void setup()
    {
        changeset = new Changeset(1, "node", "message", new Date());
    }

    @Test
    public void testSettingFilesDetailsSetsFiles()
    {
        ChangesetFileDetail fileDetails = new ChangesetFileDetail(ChangesetFileAction.ADDED, "readme.txt", 1, 2);
        changeset.setFileDetails(ImmutableList.of(fileDetails));
        assertThat(changeset.getFiles()).contains(fileDetails);
    }

    @Test
    public void testSettingFilesDetailsSetsEmptyFiles()
    {
        changeset.setFileDetails(ImmutableList.<ChangesetFileDetail>of());
        assertThat(changeset.getFiles()).isEmpty();
    }

    @Test
    public void testSettingFilesDetailsWithNull()
    {
        changeset.setFileDetails(null);
        assertThat(changeset.getFiles()).isNull();
    }
}
