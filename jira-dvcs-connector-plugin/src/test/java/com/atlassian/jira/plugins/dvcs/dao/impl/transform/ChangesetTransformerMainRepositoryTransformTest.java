package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator.GITHUB;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ChangesetTransformerMainRepositoryTransformTest extends BaseChangesetTransformerTest
{
    private static final int REPOSITORY_ID = 1239807;

    private ChangesetMapping changesetMapping;
    private RepositoryMapping repositoryMapping;

    @BeforeMethod
    public void init()
    {
        super.init();
        changesetMapping = mockChangesetMapping(0, false);

        repositoryMapping = setupActiveRepository(REPOSITORY_ID, false);
        when(changesetMapping.getRepositories()).thenReturn(new RepositoryMapping[] { repositoryMapping });
    }

    private RepositoryMapping setupActiveRepository(int id, boolean isFork)
    {
        RepositoryMapping mapping = mock(RepositoryMapping.class);
        when(mapping.isLinked()).thenReturn(true);
        when(mapping.isDeleted()).thenReturn(false);
        when(mapping.isFork()).thenReturn(isFork);
        when(mapping.getID()).thenReturn(id);
        return mapping;
    }

    @Test
    public void testWithNullMapping()
    {
        assertThat(changesetTransformer.transform(null, 0, BITBUCKET), nullValue());
    }

    @Test
    public void testWithNoRepositories()
    {
        when(changesetMapping.getRepositories()).thenReturn(new RepositoryMapping[0]);
        assertThat(changesetTransformer.transform(changesetMapping, 0, BITBUCKET), nullValue());
    }

    @Test
    public void testWithDeletedRepository()
    {
        when(repositoryMapping.isDeleted()).thenReturn(true);
        assertThat(changesetTransformer.transform(changesetMapping, 0, BITBUCKET), nullValue());
    }

    @Test
    public void testWithUnlinkedRepository()
    {
        when(repositoryMapping.isLinked()).thenReturn(false);
        assertThat(changesetTransformer.transform(changesetMapping, 0, BITBUCKET), nullValue());
    }

    @Test
    public void testWithSingleRepositoryInitialisesRepositoryId()
    {
        Changeset changeset = changesetTransformer.transform(changesetMapping, 0, null);
        assertThat(changeset.getRepositoryId(), is(REPOSITORY_ID));
    }

    @Test
    public void testWithSingleForkRepositoryInitialisesRepositoryId()
    {
        when(repositoryMapping.isFork()).thenReturn(true);
        Changeset changeset = changesetTransformer.transform(changesetMapping, 0, null);
        assertThat(changeset.getRepositoryId(), is(REPOSITORY_ID));
    }

    @Test
    public void testWithTwoRepositoryInitialisesRepositoryIdToNonFork()
    {
        final int forkRepositoryId = 777;
        RepositoryMapping forkMapping = setupActiveRepository(forkRepositoryId, true);
        when(changesetMapping.getRepositories()).thenReturn(new RepositoryMapping[] { repositoryMapping, forkMapping });

        Changeset changeset = changesetTransformer.transform(changesetMapping, 0, null);
        assertThat(changeset.getRepositoryId(), is(REPOSITORY_ID));
        assertThat(changeset.getRepositoryIds(), containsInAnyOrder(REPOSITORY_ID, forkRepositoryId));
    }

    @Test
    public void testWithTwoRepositories()
    {
        final int secondRepositoryId = 9980;
        RepositoryMapping secondMapping = setupActiveRepository(secondRepositoryId, false);
        when(changesetMapping.getRepositories()).thenReturn(new RepositoryMapping[] { repositoryMapping, secondMapping });

        Changeset changeset = changesetTransformer.transform(changesetMapping, 0, null);
        assertThat(changeset.getRepositoryId(), is(REPOSITORY_ID));
        assertThat(changeset.getRepositoryIds(), containsInAnyOrder(REPOSITORY_ID, secondRepositoryId));
    }

    @Test
    public void testWithTwoRepositoriesWrongDvcsType()
    {
        final Integer organizationId1 = 3322;
        when(repositoryMapping.getOrganizationId()).thenReturn(organizationId1);

        OrganizationMapping organization1 = mock(OrganizationMapping.class);
        when(organization1.getDvcsType()).thenReturn(BITBUCKET);

        when(activeObjects.get(OrganizationMapping.class, organizationId1)).thenReturn(organization1);

        final int secondRepositoryId = 9980;
        RepositoryMapping secondMapping = setupActiveRepository(secondRepositoryId, false);

        final Integer organizationId2 = 4455;
        when(secondMapping.getOrganizationId()).thenReturn(organizationId2);

        OrganizationMapping organization2 = mock(OrganizationMapping.class);
        when(organization2.getDvcsType()).thenReturn(GITHUB);

        when(activeObjects.get(OrganizationMapping.class, organizationId2)).thenReturn(organization2);

        when(changesetMapping.getRepositories()).thenReturn(new RepositoryMapping[] { repositoryMapping, secondMapping });

        Changeset changeset = changesetTransformer.transform(changesetMapping, 0, BITBUCKET);
        assertThat(changeset.getRepositoryId(), is(REPOSITORY_ID));
        assertThat(changeset.getRepositoryIds(), containsInAnyOrder(REPOSITORY_ID));
    }
}
