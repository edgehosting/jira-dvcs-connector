package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.testng.Assert;
import org.testng.annotations.Test;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChangesetTransformerTest extends BaseChangesetTransformerTest
{
    @Captor
    private ArgumentCaptor<Changeset> changesetArgumentCaptor;

    /**
     * Testing migration of files data for Bitbucket
     */
    @Test
    public void testNumberOfFilesFixBitbucket()
    {
        ChangesetMapping changesetMapping = mockChangesetMapping(1, true);

        Changeset changeset = changesetTransformer.transform(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
        verify(changesetDao).update(changesetArgumentCaptor.capture());

        Assert.assertEquals(changesetArgumentCaptor.getValue().getAllFileCount(), 1);
        Assert.assertNotNull(changesetArgumentCaptor.getValue().getFileDetails());

        Assert.assertEquals(changeset.getAllFileCount(), 1);
        Assert.assertNotNull(changeset.getFileDetails());
    }

    /**
     * Testing migration of files data for Github
     */
    @Test
    public void testNumberOfFilesFixGithub()
    {
        ChangesetMapping changesetMapping = mockChangesetMapping(1, true);

        Changeset changeset = changesetTransformer.transform(1, changesetMapping, GithubCommunicator.GITHUB);
        verify(changesetDao).update(changesetArgumentCaptor.capture());

        Assert.assertEquals(changesetArgumentCaptor.getValue().getAllFileCount(), 1);
        Assert.assertNotNull(changesetArgumentCaptor.getValue().getFileDetails());

        Assert.assertEquals(changeset.getAllFileCount(), 1);
        Assert.assertNotNull(changeset.getFileDetails());
    }

    /**
     * Testing migration of files data for Bitbucket when more changed files than maximum visible
     */
    @Test
    public void testNumberOfFilesFix_moreChangesBitbucket()
    {
        ChangesetMapping changesetMapping = mockChangesetMapping(Changeset.MAX_VISIBLE_FILES + 1, true);

        Changeset changeset = changesetTransformer.transform(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
        verify(changesetDao).update(changesetArgumentCaptor.capture());

        Assert.assertEquals(changesetArgumentCaptor.getValue().getAllFileCount(), Changeset.MAX_VISIBLE_FILES + 1);
        // forcing file details to reload
        Assert.assertNull(changesetArgumentCaptor.getValue().getFileDetails());

        Assert.assertEquals(changeset.getAllFileCount(), Changeset.MAX_VISIBLE_FILES + 1);
        Assert.assertNull(changeset.getFileDetails());
    }

    /**
     * Testing migration of files data for Github when more changed files than maximum visible
     */
    @Test
    public void testNumberOfFilesFix_moreChangesGithub()
    {
        ChangesetMapping changesetMapping = mockChangesetMapping(Changeset.MAX_VISIBLE_FILES + 1, true);

        Changeset changeset = changesetTransformer.transform(1, changesetMapping, GithubCommunicator.GITHUB);
        verify(changesetDao).update(changesetArgumentCaptor.capture());

        Assert.assertEquals(changesetArgumentCaptor.getValue().getAllFileCount(), Changeset.MAX_VISIBLE_FILES + 1);
        // Github doesn't need the fix
        Assert.assertNotNull(changesetArgumentCaptor.getValue().getFileDetails());

        Assert.assertEquals(changeset.getAllFileCount(), Changeset.MAX_VISIBLE_FILES + 1);
        Assert.assertNotNull(changeset.getFileDetails());
        Assert.assertEquals(changeset.getFileDetails().size(), Changeset.MAX_VISIBLE_FILES);
    }

    /**
     * Testing transformation after migration (files data is null)
     */
    @Test
    public void testNumberOfFilesFix_noFilesData()
    {
        ChangesetMapping changesetMapping = mockChangesetMapping(8, false);

        Changeset changeset = changesetTransformer.transform(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
        verify(changesetDao, never()).update(any(Changeset.class));

        Assert.assertEquals(changeset.getAllFileCount(), 8);
        Assert.assertNotNull(changeset.getFileDetails());
        Assert.assertEquals(changeset.getFileDetails().size(), Changeset.MAX_VISIBLE_FILES);
    }

    /**
     * Testing transformation after migration (files data is null) for commit with zero changed files
     */
    @Test
    public void testNumberOfFilesFixBranchCommit_noFilesData()
    {
        ChangesetMapping changesetMapping = mockChangesetMapping(0, false);

        Changeset changeset = changesetTransformer.transform(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
        verify(changesetDao, never()).update(any(Changeset.class));

        Assert.assertEquals(changeset.getAllFileCount(), 0);
        Assert.assertNotNull(changeset.getFileDetails());
        Assert.assertEquals(changeset.getFileDetails().size(), 0);
    }

    /**
     * Testing migration of files data for commit with zero changed files
     */
    @Test
    public void testNumberOfFilesFixBranchCommit()
    {
        ChangesetMapping changesetMapping = mockChangesetMapping(0, true);

        Changeset changeset = changesetTransformer.transform(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
        verify(changesetDao).update(changesetArgumentCaptor.capture());

        Assert.assertEquals(changesetArgumentCaptor.getValue().getAllFileCount(), 0);
        Assert.assertNotNull(changesetArgumentCaptor.getValue().getFileDetails());

        Assert.assertEquals(changeset.getAllFileCount(), 0);
        Assert.assertNotNull(changeset.getFileDetails());
        Assert.assertEquals(changeset.getFileDetails().size(), 0);
    }

    /**
     * Testing migration of files data to file details
     */
    @Test
    public void testFileDataToFileDetails()
    {
        ChangesetMapping changesetMapping = mock(ChangesetMapping.class);
        when(changesetMapping.getFilesData()).thenReturn("{\"count\":1,\"files\":[{\"filename\":\"file\", \"status\":\"MODIFIED\", \"additions\":1, \"deletions\":2}]}");

        Changeset changeset = changesetTransformer.transform(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
        verify(changesetDao).update(changesetArgumentCaptor.capture());

        Assert.assertEquals(changesetArgumentCaptor.getValue().getAllFileCount(), 1);
        Assert.assertNotNull(changesetArgumentCaptor.getValue().getFileDetails());

        Assert.assertEquals(changeset.getAllFileCount(), 1);
        Assert.assertNotNull(changeset.getFileDetails());
        Assert.assertEquals(changeset.getFileDetails().size(), 1);
        Assert.assertEquals(changeset.getFileDetails().get(0).getAdditions(), 1);
        Assert.assertEquals(changeset.getFileDetails().get(0).getDeletions(), 2);
        Assert.assertEquals(changeset.getFileDetails().get(0).getFile(), "file");
        Assert.assertEquals(changeset.getFileDetails().get(0).getFileAction(), ChangesetFileAction.MODIFIED);
    }
}
