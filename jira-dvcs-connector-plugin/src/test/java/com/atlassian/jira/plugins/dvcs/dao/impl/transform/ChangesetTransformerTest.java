package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChangesetTransformerTest
{
    @Mock
    private ActiveObjects activeObjects;

    @Mock
    private ChangesetDao changesetDao;

    @Captor
    private ArgumentCaptor<Changeset> changesetArgumentCaptor;

    private ChangesetTransformer changesetTransformer;

    @BeforeMethod
    public void init()
    {
        MockitoAnnotations.initMocks(this);
        changesetTransformer = new ChangesetTransformer(activeObjects, changesetDao);
    }

    @Test
    public void testNumberOfFilesFix()
    {
        ChangesetMapping changesetMapping = mockChangesetMapping();

        changesetTransformer.transform(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
        verify(changesetDao).update(changesetArgumentCaptor.capture());

        Assert.assertEquals(changesetArgumentCaptor.getValue().getAllFileCount(), 1);
        Assert.assertNotNull(changesetArgumentCaptor.getValue().getFileDetails());
    }

    private ChangesetMapping mockChangesetMapping()
    {
        ChangesetMapping changesetMapping = mock(ChangesetMapping.class);
        when(changesetMapping.getFileCount()).thenReturn(0);
        when(changesetMapping.getFileDetailsJson()).thenReturn("[{\"fileAction\":\"MODIFIED\",\"file\":\"file\",\"additions\":1,\"deletions\":1}]");
        when(changesetMapping.getFilesData()).thenReturn("{\"count\":1}");
        when(changesetMapping.getNode()).thenReturn("111111111111111");
        return changesetMapping;
    }

    @Test
    public void testNumberOfFilesFix_moreChanges()
    {
        ChangesetMapping changesetMapping = mockChangesetMappingWithMoreChanges();

        changesetTransformer.transform(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
        verify(changesetDao).update(changesetArgumentCaptor.capture());

        Assert.assertEquals(changesetArgumentCaptor.getValue().getAllFileCount(), 6);
        // forcing file details to reload
        Assert.assertNull(changesetArgumentCaptor.getValue().getFileDetails());
    }

    private ChangesetMapping mockChangesetMappingWithMoreChanges()
    {
        ChangesetMapping changesetMapping = mock(ChangesetMapping.class);
        when(changesetMapping.getFileCount()).thenReturn(0);
        when(changesetMapping.getFileDetailsJson()).thenReturn("[{\"fileAction\":\"MODIFIED\",\"file\":\"file\",\"additions\":1,\"deletions\":0},{\"fileAction\":\"MODIFIED\",\"file\":\"file2\",\"additions\":1,\"deletions\":0},{\"fileAction\":\"MODIFIED\",\"file\":\"file3\",\"additions\":1,\"deletions\":0},{\"fileAction\":\"MODIFIED\",\"file\":\"file4\",\"additions\":1,\"deletions\":0},{\"fileAction\":\"MODIFIED\",\"file\":\"file5\",\"additions\":1,\"deletions\":0}]");
        when(changesetMapping.getFilesData()).thenReturn("{\"count\":6}");
        when(changesetMapping.getNode()).thenReturn("111111111111111");
        return changesetMapping;
    }
}
