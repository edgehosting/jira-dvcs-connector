package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.List;

import net.java.ao.Query;

import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryToChangesetMapping;
import com.google.common.collect.Lists;

public class ChangesetTransformerTest
{
    @Mock
    private ActiveObjects aoMock;

    @InjectMocks
    private ChangesetTransformer transformer;

    @Captor
    private ArgumentCaptor<Query> query;

    @BeforeMethod(alwaysRun = true)
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testTransformAllShouldPreloadRepositories()
    {
        final RepositoryMapping[] sampleRepos = sampleRepos();
        final OrganizationMapping sampleOrg = sampleOrg();

        when(aoMock.find(eq(RepositoryMapping.class), isA(Query.class))).thenReturn(sampleRepos);
        when(aoMock.get(OrganizationMapping.class, 8)).thenReturn(sampleOrg);

        transformer.transformAll(changesetMappings(), 0, "bitbucket");

        verify(aoMock, atLeastOnce()).get(OrganizationMapping.class, 8);
        verify(aoMock, times(1)).find(eq(RepositoryMapping.class), query.capture());
        verifyNoMoreInteractions(aoMock);

        assertThat(query.getValue().getWhereClause(), is("ID IN (1, 2, 3, 4, 5, 6)"));
    }

    private OrganizationMapping sampleOrg()
    {
        final OrganizationMapping orgSampleMock = Mockito.mock(OrganizationMapping.class);
        when(orgSampleMock.getDvcsType()).thenReturn("bitbucket");
        return orgSampleMock;
    }

    private RepositoryMapping[] sampleRepos()
    {
        final List<RepositoryMapping> repos = Lists.newArrayList();
        for (int i = 1; i <= 6; i++)
        {
            repos.add(repoMock(Mockito.mock(RepositoryMapping.class), i));
        }
        return repos.toArray(new RepositoryMapping[] {});
    }

    private RepositoryMapping repoMock(final RepositoryMapping mock, final int id)
    {
        when(mock.isDeleted()).thenReturn(false);
        when(mock.isLinked()).thenReturn(true);
        when(mock.getID()).thenReturn(id);
        when(mock.getOrganizationId()).thenReturn(8);
        return mock;
    }

    private Collection<ChangesetMapping> changesetMappings()
    {
        final ChangesetMapping csetSampleMock = Mockito.mock(ChangesetMapping.class);
        final RepositoryToChangesetMapping[] linkMappingsSampleMock = linkMappings(1, 2, 3);
        when(csetSampleMock.getRepositoryIdMappings()).thenReturn(linkMappingsSampleMock);

        final ChangesetMapping cset2SampleMock = Mockito.mock(ChangesetMapping.class);
        final RepositoryToChangesetMapping[] link2MappingsSampleMock = linkMappings(3, 4, 5, 6);
        when(cset2SampleMock.getRepositoryIdMappings()).thenReturn(link2MappingsSampleMock);
        return Lists.newArrayList(csetSampleMock, cset2SampleMock);
    }

    private RepositoryToChangesetMapping[] linkMappings(final Integer... repoIds)
    {
        final List<RepositoryToChangesetMapping> repos = Lists.newArrayList();
        for (final Integer repoId : repoIds)
        {
            final RepositoryToChangesetMapping linkSampleMock = Mockito.mock(RepositoryToChangesetMapping.class);
            when(linkSampleMock.getRepositoryId()).thenReturn(repoId);
            repos.add(linkSampleMock);

        }
        return repos.toArray(new RepositoryToChangesetMapping[] {});
    }
}
