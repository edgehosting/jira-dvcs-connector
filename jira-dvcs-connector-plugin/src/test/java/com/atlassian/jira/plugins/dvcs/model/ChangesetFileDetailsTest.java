package com.atlassian.jira.plugins.dvcs.model;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.List;

import com.google.common.collect.ImmutableList;
import org.testng.annotations.Test;

public class ChangesetFileDetailsTest
{
    private final List<ChangesetFileDetail> fileDetails = ImmutableList.of(
            new ChangesetFileDetail(ChangesetFileAction.MODIFIED, "dir1/file1", 1, 0),
            new ChangesetFileDetail(ChangesetFileAction.MODIFIED, "dir1/file2", 0, 1)
    );

    private final String fileDetailsJson = "[{\"fileAction\":\"MODIFIED\",\"file\":\"dir1/file1\",\"additions\":1,\"deletions\":0},{\"fileAction\":\"MODIFIED\",\"file\":\"dir1/file2\",\"additions\":0,\"deletions\":1}]";

    @Test
    public void toJsonShouldHandleNull() throws Exception
    {
        assertThat(ChangesetFileDetails.toJSON(null), equalTo(null));
    }

    @Test
    public void toJsonShouldHandleList() throws Exception
    {
        assertThat(ChangesetFileDetails.toJSON(fileDetails), equalTo(fileDetailsJson));
    }

    @Test
    public void fromJsonShouldHandleNull() throws Exception
    {
        assertThat(ChangesetFileDetails.fromJSON(null), equalTo(null));
    }

    @Test
    public void fromJsonShouldHandleList() throws Exception
    {
        assertThat(ChangesetFileDetails.fromJSON(fileDetailsJson), equalTo(fileDetails));
    }
}
