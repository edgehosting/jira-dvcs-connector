package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.ChangesetMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.ChangesetDaoImpl;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator.GITHUB;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChangesetTransformerTest
{
    private static final int REPOSITORY_ID = 1239807;

    @Mock
    private ActiveObjects activeObjects;

    @Mock
    private ChangesetDaoImpl changesetDao;

    @Captor
    private ArgumentCaptor<Changeset> changesetArgumentCaptor;

    private ChangesetTransformer changesetTransformer;

    private ChangesetMapping changesetMapping;
    private RepositoryMapping repositoryMapping;

    @BeforeMethod
    public void init()
    {
        MockitoAnnotations.initMocks(this);
        changesetTransformer = new ChangesetTransformer(changesetDao);

        changesetMapping = mockChangesetMapping(0, false);

        repositoryMapping = setupActiveRepository(REPOSITORY_ID, false);
        when(changesetMapping.getRepositories()).thenReturn(new RepositoryMapping[] { repositoryMapping });
    }

    private ChangesetMapping mockChangesetMapping(int fileCount, boolean withFilesData)
    {
        ChangesetMapping changesetMapping = mock(ChangesetMapping.class);
        if (withFilesData)
        {
            when(changesetMapping.getFileCount()).thenReturn(0);
        }
        else
        {
            when(changesetMapping.getFileCount()).thenReturn(fileCount);
        }
        StringBuilder sb = new StringBuilder();
        sb.append("[");
        for (int i = 0; i < Math.min(fileCount, Changeset.MAX_VISIBLE_FILES); i++)
        {
            if (i > 0)
            {
                sb.append(",");
            }
            sb.append("{\"fileAction\":\"MODIFIED\",\"file\":\"file").append(i + 1).append("\",\"additions\":1,\"deletions\":1}");
        }
        sb.append("]");
        when(changesetMapping.getFileDetailsJson()).thenReturn(sb.toString());
        if (withFilesData)
        {
            when(changesetMapping.getFilesData()).thenReturn("{\"count\":" + fileCount + "}");
        }
        when(changesetMapping.getNode()).thenReturn("111111111111111");
        return changesetMapping;
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

    /**
     * Testing migration of files data for Bitbucket
     */
    @Test
    public void testNumberOfFilesFixBitbucket()
    {
        ChangesetMapping changesetMapping = mockChangesetMapping(1, true);

        Changeset changeset = changesetTransformer.buildChangeSet(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
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

        Changeset changeset = changesetTransformer.buildChangeSet(1, changesetMapping, GithubCommunicator.GITHUB);
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

        Changeset changeset = changesetTransformer.buildChangeSet(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
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

        Changeset changeset = changesetTransformer.buildChangeSet(1, changesetMapping, GithubCommunicator.GITHUB);
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

        Changeset changeset = changesetTransformer.buildChangeSet(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
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

        Changeset changeset = changesetTransformer.buildChangeSet(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
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

        Changeset changeset = changesetTransformer.buildChangeSet(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
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

        Changeset changeset = changesetTransformer.buildChangeSet(1, changesetMapping, BitbucketCommunicator.BITBUCKET);
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
