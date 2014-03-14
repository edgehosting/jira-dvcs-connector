package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import com.google.common.collect.ObjectArrays;
import net.java.ao.Query;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ChangesetTransformerTest
{
    @Mock
    private ActiveObjects aoMock;

    @InjectMocks
    private ChangesetTransformer transformer;

    @BeforeMethod(alwaysRun = true)
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testTransformAllShouldPreloadRepositories()
    {
        when(aoMock.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(
                new Answer<Object>()
                {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public Object answer(InvocationOnMock invocationOnMock) throws Throwable
                    {
                        return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
                    }
                });

        transformer.transformAll(changesetMappings(), 0, "bitbucket");

        verify(aoMock).executeInTransaction(any(TransactionCallback.class));
        verify(aoMock).find(eq(RepositoryToChangesetMapping.class), any(Query.class));
        verifyNoMoreInteractions(aoMock);
    }

    private Collection<ChangesetMapping> changesetMappings()
    {
        final ChangesetMapping csetSampleMock = Mockito.mock(ChangesetMapping.class);
        final RepositoryToChangesetMapping[] linkMappingsSampleMock = linkMappings(csetSampleMock, 1, 2, 3);
        when(csetSampleMock.getRepositoryIdMappings()).thenReturn(linkMappingsSampleMock);

        final ChangesetMapping cset2SampleMock = Mockito.mock(ChangesetMapping.class);
        final RepositoryToChangesetMapping[] link2MappingsSampleMock = linkMappings(cset2SampleMock, 3, 4, 5, 6);
        when(cset2SampleMock.getRepositoryIdMappings()).thenReturn(link2MappingsSampleMock);

        when(aoMock.find(eq(RepositoryToChangesetMapping.class), isA(Query.class))).thenReturn(ObjectArrays.concat(linkMappingsSampleMock, link2MappingsSampleMock, RepositoryToChangesetMapping.class));

        return Lists.newArrayList(csetSampleMock, cset2SampleMock);
    }

    private RepositoryToChangesetMapping[] linkMappings(final ChangesetMapping changesetMapping, final Integer... repoIds)
    {
        final List<RepositoryToChangesetMapping> repos = Lists.newArrayList();
        for (final Integer repoId : repoIds)
        {
            final RepositoryToChangesetMapping linkSampleMock = Mockito.mock(RepositoryToChangesetMapping.class);
            when(linkSampleMock.getRepositoryId()).thenReturn(repoId);
            when(linkSampleMock.getChangeset()).thenReturn(changesetMapping);
            repos.add(linkSampleMock);

        }
        return repos.toArray(new RepositoryToChangesetMapping[] {});
    }
}
