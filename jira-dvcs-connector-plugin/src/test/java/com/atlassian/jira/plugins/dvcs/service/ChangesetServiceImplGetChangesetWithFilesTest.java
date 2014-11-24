package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileAction;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetail;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFileDetailsEnvelope;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.google.common.collect.ImmutableList;
import org.hamcrest.MatcherAssert;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator.BITBUCKET;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.when;

public class ChangesetServiceImplGetChangesetWithFilesTest
{
    private static final int REPOSITORY_ID = 33;
    private static final String WITH_DATA_NODE = "123-sdkj";
    private static final String NO_DATA_NODE = "9834lk-gffg";

    @Mock
    private ClusterLockServiceFactory clusterLockServiceFactory;

    @Mock
    private ChangesetDao changesetDao;

    @Mock
    private DvcsCommunicator dvcsCommunicator;

    @Mock
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Mock
    private RepositoryDao repositoryDao;

    private Repository repository;


    private ImmutableList.Builder<Changeset> builder;

    private Changeset changesetWithData;
    private Changeset changesetWithoutData;
    private ChangesetFileDetail fileDetail;

    private List<Changeset> changesets;

    private ChangesetServiceImpl changesetService;

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);

        changesetService = new ChangesetServiceImpl(null, clusterLockServiceFactory);
        ReflectionTestUtils.setField(changesetService, "changesetDao", changesetDao);
        ReflectionTestUtils.setField(changesetService, "repositoryDao", repositoryDao);
        ReflectionTestUtils.setField(changesetService, "dvcsCommunicatorProvider", dvcsCommunicatorProvider);

        repository = new Repository();
        repository.setId(REPOSITORY_ID);
        repository.setDvcsType(BITBUCKET);

        changesets = new ArrayList<Changeset>();

        builder = ImmutableList.builder();

        changesetWithData = new Changeset(REPOSITORY_ID, WITH_DATA_NODE, "something", new Date());
        changesetWithData.setAllFileCount(1);
        changesetWithData.setFileDetails(new LinkedList<ChangesetFileDetail>());

        changesetWithoutData = new Changeset(REPOSITORY_ID, NO_DATA_NODE, "nohting", new Date());

        fileDetail = new ChangesetFileDetail(ChangesetFileAction.ADDED, "foo.txt", 1, 0);
    }

    @Test
    public void testHappyPathWithExistingData() throws Exception
    {
        when(repositoryDao.get(REPOSITORY_ID)).thenReturn(repository);
        when(dvcsCommunicatorProvider.getCommunicator(BITBUCKET)).thenReturn(dvcsCommunicator);
        changesets.add(changesetWithData);

        List<Changeset> result = changesetService.getChangesetsWithFileDetails(ImmutableList.of(changesetWithData));

        MatcherAssert.assertThat(result, containsInAnyOrder(changesetWithData));
    }

    @Test
    public void testChangesetWithFileDetailsIsNotMigrated() throws Exception
    {
        changesets.add(changesetWithData);
        changesetService.processRepository(repository, changesets, dvcsCommunicator, builder);

        final ImmutableList<Changeset> processedChangesets = builder.build();
        MatcherAssert.assertThat(processedChangesets, containsInAnyOrder(changesetWithData));
    }

    @Test
    public void testChangesetWithoutFileDetailsIsMigratedFromDB() throws Exception
    {
        changesets.add(changesetWithoutData);

        when(changesetDao.migrateFilesData(changesetWithoutData, BITBUCKET)).thenAnswer(new Answer<Changeset>()
        {
            @Override
            public Changeset answer(final InvocationOnMock invocation) throws Throwable
            {
                Changeset suppliedChangeset = (Changeset) invocation.getArguments()[0];
                final List<ChangesetFileDetail> detailsList = new ArrayList<ChangesetFileDetail>();
                detailsList.add(fileDetail);
                suppliedChangeset.setFileDetails(detailsList);

                return suppliedChangeset;
            }
        });

        changesetService.processRepository(repository, changesets, dvcsCommunicator, builder);

        final ImmutableList<Changeset> processedChangesets = builder.build();
        MatcherAssert.assertThat(processedChangesets.size(), equalTo(1));
        MatcherAssert.assertThat(processedChangesets.get(0).getFileDetails(), containsInAnyOrder(fileDetail));
    }

    @Test
    public void testChangesetWithoutFileDetailsIsMigratedFromBB() throws Exception
    {
        changesets.add(changesetWithoutData);

        when(changesetDao.migrateFilesData(changesetWithoutData, BITBUCKET)).thenReturn(changesetWithoutData);
        final ChangesetFileDetailsEnvelope changesetFileDetailsEnvelope = new ChangesetFileDetailsEnvelope(ImmutableList.of(fileDetail), 1);
        when(dvcsCommunicator.getFileDetails(repository, changesetWithoutData)).thenReturn(changesetFileDetailsEnvelope);
        when(changesetDao.update(changesetWithoutData)).thenReturn(changesetWithoutData);

        changesetService.processRepository(repository, changesets, dvcsCommunicator, builder);

        final ImmutableList<Changeset> processedChangesets = builder.build();
        MatcherAssert.assertThat(processedChangesets.size(), equalTo(1));
        final Changeset processedChangeset = processedChangesets.get(0);
        MatcherAssert.assertThat(processedChangeset.getFileDetails(), containsInAnyOrder(fileDetail));
        MatcherAssert.assertThat(processedChangeset.getAllFileCount(), equalTo(1));
    }

    @Test
    public void testChangesetWithoutFileDetailsStillInResultWhenExceptionThrown() throws Exception
    {
        changesets.add(changesetWithoutData);

        when(changesetDao.migrateFilesData(changesetWithoutData, BITBUCKET)).thenReturn(changesetWithoutData);
        when(dvcsCommunicator.getFileDetails(repository, changesetWithoutData)).thenThrow(new SourceControlException());

        changesetService.processRepository(repository, changesets, dvcsCommunicator, builder);

        final ImmutableList<Changeset> processedChangesets = builder.build();
        MatcherAssert.assertThat(processedChangesets, containsInAnyOrder(changesetWithoutData));
    }
}
