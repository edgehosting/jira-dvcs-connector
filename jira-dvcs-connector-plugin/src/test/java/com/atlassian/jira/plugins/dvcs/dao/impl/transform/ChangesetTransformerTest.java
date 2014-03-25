package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.sal.api.transaction.TransactionCallback;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChangesetTransformerTest
{
    @Mock
    private ActiveObjects activeObjects;

    @Mock
    private ChangesetDao changesetDao;

    @InjectMocks
    private ChangesetTransformer changesetTransformer;

    @Captor
    private ArgumentCaptor<Changeset> changesetArgumentCaptor;

    @BeforeMethod
    public void init()
    {
        MockitoAnnotations.initMocks(this);

        when(activeObjects.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
            }
        });
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
