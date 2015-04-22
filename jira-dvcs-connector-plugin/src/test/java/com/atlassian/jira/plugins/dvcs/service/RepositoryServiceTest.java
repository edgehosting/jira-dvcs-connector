package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.beehive.ClusterLock;
import com.atlassian.beehive.ClusterLockService;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.event.CarefulEventService;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryRegistration;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.sal.api.pluginsettings.PluginSettings;
import com.atlassian.sal.api.pluginsettings.PluginSettingsFactory;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.atlassian.jira.plugins.dvcs.service.RepositoryServiceImpl.SYNC_REPOSITORY_LIST_LOCK;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class RepositoryServiceTest
{
    @Mock
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Mock
    private RepositoryDao repositoryDao;

    @Mock
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Mock
    private BranchService branchService;

    @Mock
    private Synchronizer synchronizer;

    @Mock
    private ChangesetService changesetService;

    @Mock
    private ApplicationProperties applicationProperties;

    @Mock
    private DvcsCommunicator bitbucketCommunicator;

    @Mock
    private PluginSettingsFactory settings;

    @Mock
    private Synchronizer synchroizerMock;

    @Mock
    private SyncAuditLogDao syncAudit;

    @Mock
    private GitHubEventService gitHubEventService;

    @Mock
    private ClusterLockService clusterLockService;

    @Mock
    CarefulEventService eventService;

    @Mock
    Repository repository;

    // tested object
    @InjectMocks
    private RepositoryServiceImpl repositoryService = new RepositoryServiceImpl();

    public RepositoryServiceTest()
    {
        super();
    }

    @BeforeMethod
    public void setup()
    {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDestroy() throws Exception
    {
        BlockingQueue<Runnable> queue = mock(BlockingQueue.class);
        ThreadPoolExecutor executor = mock(ThreadPoolExecutor.class);
        when(executor.getQueue()).thenReturn(queue);

        repositoryService.init(executor);
        repositoryService.destroy();

        verify(executor).shutdown();
        verify(executor, never()).shutdownNow();
        verify(queue).clear();
        verify(executor).awaitTermination(anyLong(), any(TimeUnit.class));
    }

    @Test
    public void testDisableRepository()
    {

        Repository sampleRepository = createSampleRepository();
        when(repositoryDao.get(0)).thenReturn(sampleRepository);
        when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
        when(applicationProperties.getBaseUrl()).thenReturn("https://myjira.org");

        RepositoryRegistration registration = repositoryService.enableRepository(0, false);

        verify(repositoryDao).save(sampleRepository);
        verify(bitbucketCommunicator).removePostcommitHook(Mockito.eq(sampleRepository),
                Mockito.eq(createPostcommitUrl(sampleRepository)));
        Assert.assertFalse(registration.isCallBackUrlInstalled());
        Assert.assertEquals(registration.getRepository(), sampleRepository);
    }

    @Test
    public void testDisableRepositoryWithoutAdminRights()
    {
        Repository sampleRepository = createSampleRepository();
        when(repositoryDao.get(0)).thenReturn(sampleRepository);
        when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
        Mockito.doThrow(new SourceControlException.PostCommitHookRegistrationException("", null)).when(bitbucketCommunicator).removePostcommitHook(Mockito.any(Repository.class), Mockito.anyString());
        when(applicationProperties.getBaseUrl()).thenReturn("https://myjira.org");

        RepositoryRegistration registration = repositoryService.enableRepository(0, false);

        verify(repositoryDao).save(sampleRepository);
        verify(bitbucketCommunicator).removePostcommitHook(Mockito.eq(sampleRepository),
                Mockito.eq(createPostcommitUrl(sampleRepository)));
        Assert.assertTrue(registration.isCallBackUrlInstalled());
        Assert.assertEquals(registration.getRepository(), sampleRepository);
    }

    @Test
    public void testEnableRepository()
    {

        Repository sampleRepository = createSampleRepository();
        when(repositoryDao.get(0)).thenReturn(sampleRepository);
        when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
        when(applicationProperties.getBaseUrl()).thenReturn("https://myjira.org");

        RepositoryRegistration registration = repositoryService.enableRepository(0, true);

        verify(repositoryDao).save(sampleRepository);
        verify(bitbucketCommunicator).ensureHookPresent(Mockito.eq(sampleRepository),
                Mockito.eq(createPostcommitUrl(sampleRepository)));
        Assert.assertTrue(registration.isCallBackUrlInstalled());
        Assert.assertNotNull(registration.getCallBackUrl());
        Assert.assertEquals(registration.getRepository(), sampleRepository);
    }

    @Test
    public void testEnableRepositoryWithoutAdminRights()
    {

        Repository sampleRepository = createSampleRepository();
        when(repositoryDao.get(0)).thenReturn(sampleRepository);
        when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
        Mockito.doThrow(new SourceControlException.PostCommitHookRegistrationException("", null)).when(bitbucketCommunicator).ensureHookPresent(Mockito.any(Repository.class), Mockito.anyString());
        when(applicationProperties.getBaseUrl()).thenReturn("https://myjira.org");

        RepositoryRegistration registration = repositoryService.enableRepository(0, true);

        verify(repositoryDao).save(sampleRepository);
        verify(bitbucketCommunicator).ensureHookPresent(Mockito.eq(sampleRepository),
                Mockito.eq(createPostcommitUrl(sampleRepository)));
        Assert.assertFalse(registration.isCallBackUrlInstalled());
        Assert.assertNotNull(registration.getCallBackUrl());
        Assert.assertEquals(registration.getRepository(), sampleRepository);
    }

    @Test
    public void testSyncRepositoryList()
    {
        when(settings.createGlobalSettings()).thenReturn(mock(PluginSettings.class));
        Repository sampleRepository1 = createSampleRepository();
        sampleRepository1.setId(1);
        sampleRepository1.setSlug("sampleRepository1");

        Repository sampleRepository2 = createSampleRepository();
        sampleRepository2.setId(2);
        sampleRepository2.setSlug("sampleRepository2");

        Repository sampleRepository3 = createSampleRepository();
        sampleRepository3.setId(3);
        sampleRepository3.setSlug("sampleRepository3");

        Repository sampleRepository4 = createSampleRepository();
        sampleRepository4.setId(4);
        sampleRepository4.setSlug("sampleRepository4");

        List<Repository> storedRepos = new ArrayList<Repository>();
        storedRepos.add(sampleRepository1);
        storedRepos.add(sampleRepository2);
        List<Repository> remoteRepos = new ArrayList<Repository>();
        // first one deleted, 3, 4 added
        remoteRepos.add(sampleRepository2);
        remoteRepos.add(sampleRepository3);
        remoteRepos.add(sampleRepository4);

        Organization sampleOrganization = new Organization();
        sampleOrganization.setId(5);
        sampleOrganization.setDvcsType("bitbucket");
        sampleOrganization.setAutolinkNewRepos(true);

        when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
        when(bitbucketCommunicator.getRepositories(sampleOrganization, storedRepos)).thenReturn(remoteRepos);
        when(repositoryDao.getAllByOrganization(5, true)).thenReturn(storedRepos);
        when(repositoryDao.save(sampleRepository3)).thenReturn(sampleRepository3);
        when(repositoryDao.save(sampleRepository4)).thenReturn(sampleRepository4);
        when(repositoryDao.getAllByOrganization(5, false)).thenReturn(Lists.<Repository>newArrayList(Iterables.concat(storedRepos, remoteRepos)));
        when(applicationProperties.getBaseUrl()).thenReturn("https://myjira.org");
        final ClusterLock mockLock = mock(ClusterLock.class);
        when(clusterLockService.getLockForName(SYNC_REPOSITORY_LIST_LOCK)).thenReturn(mockLock);

        repositoryService.syncRepositoryList(sampleOrganization);

        // 2 has been updated
        verify(repositoryDao, Mockito.times(1)).save(sampleRepository2);

        // 1 has been deleted
        verify(repositoryDao, Mockito.times(1)).save(Mockito.argThat(new ArgumentMatcher<Repository>()
        {
            @Override
            public boolean matches(Object argument)
            {
                Repository repo = (Repository) argument;
                return repo.getId() == 1 && repo.isDeleted();
            }
        }));

        // 3, 4 has been added
        verify(repositoryDao, Mockito.times(2)).save(Mockito.argThat(new ArgumentMatcher<Repository>()
        {
            @Override
            public boolean matches(Object argument)
            {
                Repository repo = (Repository) argument;
                return repo.getId() == 3 || repo.getId() == 4;
            }
        }));
        // ... with false linking
        verify(bitbucketCommunicator).ensureHookPresent(sampleRepository3, createPostcommitUrl(sampleRepository3));
        verify(bitbucketCommunicator).ensureHookPresent(sampleRepository4, createPostcommitUrl(sampleRepository4));
        verify(mockLock).unlock();
    }

    @Test
    public void testRemoveRepository()
    {
        Repository sampleRepository = createSampleRepository();
        sampleRepository.setId(8);

        repositoryService.remove(sampleRepository);

        verify(changesetService).removeAllInRepository(8);
        verify(repositoryDao).remove(8);
        verify(repositoryPullRequestDao).removeAll(sampleRepository);
    }

    @Test
    public void testRemoveRepositoryIsLinked()
    {
        Repository sampleRepository = createSampleRepository();
        sampleRepository.setId(8);
        sampleRepository.setLinked(true);
        when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
        when(applicationProperties.getBaseUrl()).thenReturn("https://myjira.org");

        repositoryService.remove(sampleRepository);

        verify(changesetService).removeAllInRepository(8);
        verify(repositoryDao).remove(8);

        verify(bitbucketCommunicator).removePostcommitHook(Mockito.eq(sampleRepository),
                Mockito.eq(createPostcommitUrl(sampleRepository)));
    }

    /**
     * Unit tests related to {@link RepositoryServiceImpl#getUser(Repository, String, String)}.
     */
    @Test
    public void testGetUser()
    {
        Repository repository = mock(Repository.class);
        DvcsCommunicator testCommunicator = mock(DvcsCommunicator.class);

        String dvcsType = "test-dvcs-type";

        class BooleanFlag
        {
            private boolean flag;
        }
        final BooleanFlag wasInvoked = new BooleanFlag();

        when(repository.getDvcsType()).thenReturn(dvcsType);
        when(dvcsCommunicatorProvider.getCommunicator(dvcsType)).thenReturn(testCommunicator);
        when(testCommunicator.getUser(Mockito.eq(repository), Mockito.anyString())).thenAnswer(new Answer<DvcsUser>()
        {

            @Override
            public DvcsUser answer(InvocationOnMock invocation) throws Throwable
            {
                wasInvoked.flag = true;
                return new DvcsUser((String) invocation.getArguments()[1], "", "", "", "");
            }

        });

        DvcsUser user;

        wasInvoked.flag = false;
        user = repositoryService.getUser(repository, null, null);
        Assert.assertFalse(wasInvoked.flag);
        Assert.assertTrue(user instanceof DvcsUser.UnknownUser);

        wasInvoked.flag = false;
        user = repositoryService.getUser(repository, "", null);
        Assert.assertFalse(wasInvoked.flag);
        Assert.assertTrue(user instanceof DvcsUser.UnknownUser);

        wasInvoked.flag = false;
        user = repositoryService.getUser(repository, "test", null);
        Assert.assertTrue(wasInvoked.flag);
        Assert.assertFalse(user instanceof DvcsUser.UnknownUser);
    }

    @Test
    public void removingRepositoryShouldClearAssociatedEvents() throws Exception
    {
        repositoryService.remove(repository);
        verify(eventService).discardEvents(repository);
    }

    private Repository createSampleRepository()
    {
        Repository repository = new Repository();
        repository.setName("doesnotmatter_repo");
        repository.setDvcsType("bitbucket");
        repository.setOrganizationId(1);
        repository.setSlug("doesnotmatter-repo");
        return repository;
    }

    private String createPostcommitUrl(Repository forRepo)
    {
        return "https://myjira.org" + "/rest/bitbucket/1.0/repository/" + forRepo.getId() + "/sync";
    }

}
