package com.atlassian.jira.plugins.dvcs.sync.impl;

import com.atlassian.beehive.ClusterLock;
import com.atlassian.beehive.ClusterLockService;
import com.atlassian.cache.CacheManager;
import com.atlassian.cache.memory.MemoryCacheManager;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.FeatureManager;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;
import com.atlassian.jira.plugins.dvcs.auth.OAuthStore;
import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.event.CarefulEventService;
import com.atlassian.jira.plugins.dvcs.event.EventService;
import com.atlassian.jira.plugins.dvcs.event.RepositorySync;
import com.atlassian.jira.plugins.dvcs.event.RepositorySyncHelper;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.event.ThreadPoolUtil;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.BranchServiceImpl;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.MessageExecutor;
import com.atlassian.jira.plugins.dvcs.service.MessagingServiceImplMock;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagePayloadSerializer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.plugins.dvcs.smartcommits.SmartcommitsChangesetsProcessor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilder;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranch;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranchesAndTags;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetFile;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.BranchesAndTagsRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ChangesetRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessageSerializer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync.OldBitbucketSynchronizeCsetMsgSerializer;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessageSerializer;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.GithubSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.OldBitbucketSynchronizeCsetMsgConsumer;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginInformation;
import com.atlassian.sal.api.ApplicationProperties;
import com.atlassian.util.concurrent.Promises;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import it.com.atlassian.jira.plugins.dvcs.DumbClusterLockServiceFactory;
import junit.framework.Assert;
import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.Commit;
import org.eclipse.egit.github.core.CommitFile;
import org.eclipse.egit.github.core.CommitUser;
import org.eclipse.egit.github.core.IRepositoryIdProvider;
import org.eclipse.egit.github.core.RepositoryBranch;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.TypedResource;
import org.eclipse.egit.github.core.service.CommitService;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.test.util.ReflectionTestUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.testng.collections.Sets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

import static com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag.SOFT_SYNC;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * Integration test of the DefaultSynchronizer.
 */
public class DefaultSynchronizerTest
{
    @Mock
    private EventPublisher eventPublisher;

    @Mock
    private Repository repositoryMock;
    @Mock
    private BitbucketLinker bitbucketLinker;
    @Mock
    private PluginAccessor pluginAccessor;

    @Mock
    private FeatureManager featureManager;

    @Mock
    private BitbucketClientBuilderFactory bitbucketClientBuilderFactory;

    @Mock
    private ThreadEvents threadEvents;

    @Mock
    private RepositorySyncHelper repoSyncHelper;

    /**
     * The sync given to the DefaultSynchronizer.
     */
    @Mock
    private RepositorySync repoSyncForDefaultSync;

    /**
     * The sync given to the MessageExecutor.
     */
    @Mock
    private RepositorySync repoSyncForMessageExecutor;

    @Mock
    private RepositorySync notCapturingRepoSync;

    @Mock
    private EventService eventService;

    @Mock
    private BranchService branchService;

    @InjectMocks
    private MessagingService messagingService = new MessagingServiceImplMock();

    private BranchDao branchDao;
    @Mock
    private BitbucketRemoteClient bitbucketRemoteClient;
    @Mock
    private BranchesAndTagsRemoteRestpoint branchesAndTagsRemoteRestpoint;
    @Mock
    private BitbucketBranchesAndTags bitbucketBranchesAndTags;
    @Mock
    private ChangesetRemoteRestpoint changesetRestpoint;
    @Mock
    private Plugin plugin;
    @Mock
    private PluginInformation pluginInformation;

    @Mock
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @InjectMocks
    private BitbucketSynchronizeChangesetMessageConsumer consumer;

    @InjectMocks
    private OldBitbucketSynchronizeCsetMsgConsumer oldConsumer;

    @InjectMocks
    private BitbucketSynchronizeChangesetMessageSerializer serializer;

    @InjectMocks
    private OldBitbucketSynchronizeCsetMsgSerializer oldSerializer;

    @Mock
    private ChangesetService changesetService;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private LinkedIssueService linkedIssueService;

    @Mock
    private HttpClientProvider httpClientProvider;

    @Mock
    private SmartcommitsChangesetsProcessor smartCommitsProcessor;

    @InjectMocks
    private GithubSynchronizeChangesetMessageConsumer githubConsumer;

    @InjectMocks
    private SynchronizeChangesetMessageSerializer githubSerializer;

    @Mock
    private OAuthStore oAuthStore;

    @Mock
    private GithubClientProvider githubClientProvider;

    @Mock
    private CommitService commitService;

    @Mock
    private SyncAuditLogDao syncAudit;

    @Mock
    private SyncAuditLogMapping syncAuditLogMock;

    @Mock
    private RepositoryDao repositoryDao;

    @Mock
    private ChangesetDao changesetDao;

    @Mock
    private org.eclipse.egit.github.core.service.RepositoryService egitRepositoryService;

    @Mock
    private ClusterLockService clusterLockService;

    @Mock
    private ApplicationProperties ap;

    @Mock
    private SyncDisabledHelper syncDisabledHelper;

    private final CacheManager cacheManager = new MemoryCacheManager();

    @InjectMocks
    private DefaultSynchronizer defaultSynchronizer =
            new DefaultSynchronizer(cacheManager, new DumbClusterLockServiceFactory());

    @Mock
    private ClusterLock clusterLock;

    @BeforeMethod
    public void setUp() throws Exception
    {
        // repo sync that doesn't capture
        //noinspection unchecked
        when(repoSyncHelper.startSync(any(Repository.class), (EnumSet) argThat(not(hasItem(SOFT_SYNC))))).thenReturn(notCapturingRepoSync);

        // the capturing syncs
        //noinspection unchecked
        when(repoSyncHelper.startSync(any(Repository.class), (EnumSet) argThat(hasItem(SOFT_SYNC)))).thenReturn(repoSyncForDefaultSync, repoSyncForMessageExecutor);

        when(smartCommitsProcessor.startProcess(any(Progress.class), any(Repository.class), any(ChangesetService.class))).thenReturn(Promises.<Void>promise(null));

        // wire up the DefaultSynchronizer with our mock ThreadEvents
        ReflectionTestUtils.setField(defaultSynchronizer, "repoSyncHelper", repoSyncHelper);
    }

    private static class BuilderAnswer implements Answer<Object>
    {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable
        {
            Object builderMock = invocation.getMock();
            if (invocation.getMethod().getReturnType().isInstance(builderMock))
            {
                return builderMock;
            }
            else
            {
                return Mockito.RETURNS_DEFAULTS.answer(invocation);
            }
        }
    }

    private class BranchDaoMock implements BranchDao
    {
        private final ArrayListMultimap<Integer, BranchHead> heads = ArrayListMultimap.create();
        private final ArrayListMultimap<Integer, Branch> branches = ArrayListMultimap.create();

        @Override
        public void createBranchHead(int repositoryId, BranchHead branch)
        {
            Assert.assertFalse(String.format("BranchHead %d must not exist for repository %s", repositoryId, branch), heads.containsEntry(repositoryId, branch));
            heads.put(repositoryId, branch);
        }

        @Override
        public List<BranchHead> getBranchHeads(int repositoryId)
        {
            List<BranchHead> result = heads.get(repositoryId);

            if (result == null)
            {
                return Collections.emptyList();
            }
            else
            {
                return Lists.newArrayList(result);
            }
        }

        @Override
        public void removeBranchHead(int repositoryId, BranchHead branchHead)
        {
            heads.remove(repositoryId, branchHead);
        }

        @Override
        public void removeAllBranchHeadsInRepository(int repositoryId)
        {
            heads.removeAll(repositoryId);
        }

        @Override
        public List<Branch> getBranchesForIssue(final Iterable<String> issueKeys)
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        @Override
        public List<Branch> getBranches(final int repositoryId)
        {
            return branches.get(repositoryId);
        }

        @Override
        public void createBranch(final int repositoryId, final Branch branch, final Set<String> issueKeys)
        {
            branches.put(repositoryId, branch);
        }

        @Override
        public void removeBranch(final int repositoryId, final Branch branch)
        {
            branches.remove(repositoryId, branch);
        }

        @Override
        public void removeAllBranchesInRepository(final int repositoryId)
        {
            branches.removeAll(repositoryId);
        }

        @Override
        public List<Branch> getBranchesForRepository(final int repositoryId)
        {
            return branches.get(repositoryId);
        }

        @Override
        public List<Branch> getBranchesForIssue(final Iterable<String> issueKeys, final String dvcsType)
        {
            throw new UnsupportedOperationException("Not implemented");
        }

        public List<String> getHeads(int repositoryId)
        {
            return Lists.transform(heads.get(repositoryId), new Function<BranchHead, String>()
            {
                @Override
                public String apply(BranchHead input)
                {
                    return input.getHead();
                }
            });
        }
    }

    @BeforeMethod
    public void initializeMocksAndBitbucketCommunicator()
    {
        MockitoAnnotations.initMocks(this);

        when(clusterLockService.getLockForName(DefaultSynchronizer.SYNC_LOCK)).thenReturn(clusterLock);

        when(pluginInformation.getVersion()).thenReturn("0");
        when(plugin.getPluginInformation()).thenReturn(pluginInformation);
        when(pluginAccessor.getPlugin(anyString())).thenReturn(plugin);

        when(repositoryMock.getId()).thenReturn(1);
        when(repositoryMock.isLinked()).thenReturn(true);
        when(repositoryMock.getOrgName()).thenReturn("ORG");
        when(repositoryMock.getSlug()).thenReturn("SLUG");

        branchDao = new BranchDaoMock();
        final BranchService branchService = new BranchServiceImpl();
        ReflectionTestUtils.setField(branchService, "threadEvents", threadEvents);
        ReflectionTestUtils.setField(branchService, "branchDao", branchDao);
        ReflectionTestUtils.setField(branchService, "dvcsCommunicatorProvider", dvcsCommunicatorProvider);

        when(syncAudit.newSyncAuditLog(eq(repositoryMock.getId()), anyString(), any(Date.class))).thenReturn(syncAuditLogMock);

        ReflectionTestUtils.setField(messagingService, "eventService", mock(CarefulEventService.class));

        ReflectionTestUtils.setField(defaultSynchronizer, "branchService", branchService);
        ReflectionTestUtils.setField(defaultSynchronizer, "messagingService", messagingService);
        ReflectionTestUtils.setField(defaultSynchronizer, "syncDisabledHelper", syncDisabledHelper);

        final BitbucketClientBuilder bitbucketClientBuilder = mock(BitbucketClientBuilder.class, new BuilderAnswer());

        when(bitbucketClientBuilderFactory.forRepository(Matchers.any(Repository.class))).thenReturn(bitbucketClientBuilder);

        final CachingCommunicator bitbucketCachingCommunicator = new CachingCommunicator(cacheManager);
        final CachingCommunicator githubCachingCommunicator = new CachingCommunicator(cacheManager);

        SyncDisabledHelper syncDisabledHelper = new SyncDisabledHelper();
        ReflectionTestUtils.setField(syncDisabledHelper, "featureManager", featureManager);

        final BitbucketCommunicator bitbucketCommunicator = new BitbucketCommunicator(bitbucketLinker, pluginAccessor, bitbucketClientBuilderFactory, ap);
        ReflectionTestUtils.setField(bitbucketCommunicator, "changesetDao", changesetDao);
        ReflectionTestUtils.setField(bitbucketCommunicator, "branchService", branchService);
        ReflectionTestUtils.setField(bitbucketCommunicator, "messagingService", messagingService);
        ReflectionTestUtils.setField(bitbucketCommunicator, "syncDisabledHelper", syncDisabledHelper);

        final GithubCommunicator githubCommunicator = new GithubCommunicator(oAuthStore, githubClientProvider);
        ReflectionTestUtils.setField(githubCommunicator, "branchService", branchService);
        ReflectionTestUtils.setField(githubCommunicator, "messagingService", messagingService);
        ReflectionTestUtils.setField(githubCommunicator, "syncDisabledHelper", syncDisabledHelper);

        bitbucketCachingCommunicator.setDelegate(bitbucketCommunicator);
        githubCachingCommunicator.setDelegate(githubCommunicator);

        when(githubClientProvider.getCommitService(any(Repository.class))).thenReturn(commitService);
        when(githubClientProvider.getRepositoryService(any(Repository.class))).thenReturn(egitRepositoryService);

        when(dvcsCommunicatorProvider.getCommunicator(eq(BitbucketCommunicator.BITBUCKET))).thenReturn(bitbucketCachingCommunicator);
        when(dvcsCommunicatorProvider.getCommunicator(eq(GithubCommunicator.GITHUB))).thenReturn(githubCachingCommunicator);

        when(bitbucketClientBuilder.build()).thenReturn(bitbucketRemoteClient);
        when(bitbucketRemoteClient.getChangesetsRest()).thenReturn(changesetRestpoint);
        when(bitbucketRemoteClient.getBranchesAndTagsRemoteRestpoint()).thenReturn(branchesAndTagsRemoteRestpoint);

        when(repositoryService.get(anyInt())).thenReturn(repositoryMock);

        ReflectionTestUtils.setField(consumer, "cachingCommunicator", bitbucketCachingCommunicator);
        ReflectionTestUtils.setField(consumer, "messagingService", messagingService);
        ReflectionTestUtils.setField(oldConsumer, "messagingService", messagingService);
        ReflectionTestUtils.setField(githubConsumer, "messagingService", messagingService);

        ReflectionTestUtils.setField(serializer, "messagingService", messagingService);
        ReflectionTestUtils.setField(oldSerializer, "messagingService", messagingService);
        ReflectionTestUtils.setField(githubSerializer, "messagingService", messagingService);
        ReflectionTestUtils.setField(serializer, "synchronizer", defaultSynchronizer);
        ReflectionTestUtils.setField(oldSerializer, "synchronizer", defaultSynchronizer);
        ReflectionTestUtils.setField(githubSerializer, "synchronizer", defaultSynchronizer);

        final MessageExecutor messageExecutor = new MessageExecutor(ThreadPoolUtil.newSingleThreadExecutor(ThreadFactories.namedThreadFactory("DVCSConnector.MessageExecutor")));

        ReflectionTestUtils.setField(messageExecutor, "messagingService", messagingService);
        ReflectionTestUtils.setField(messageExecutor, "clusterLockServiceFactory", new DumbClusterLockServiceFactory());
        ReflectionTestUtils.setField(messageExecutor, "consumers", new MessageConsumer<?>[] { consumer, oldConsumer, githubConsumer });
        ReflectionTestUtils.setField(messageExecutor, "repoSyncHelper", repoSyncHelper);
        ReflectionTestUtils.invokeMethod(messageExecutor, "init");

        ReflectionTestUtils.setField(messagingService, "messageConsumers", new MessageConsumer<?>[] { consumer, oldConsumer, githubConsumer });
        ReflectionTestUtils.setField(messagingService, "payloadSerializers", new MessagePayloadSerializer<?>[] { serializer, oldSerializer, githubSerializer });
        ReflectionTestUtils.setField(messagingService, "messageExecutor", messageExecutor);
        ReflectionTestUtils.setField(messagingService, "synchronizer", defaultSynchronizer);

        ReflectionTestUtils.invokeMethod(messagingService, "init");
    }

    private class Graph
    {
        private class Data
        {
            private final String branch;
            private final Date date;

            Data(String branch, Date date)
            {
                this.branch = branch;
                this.date = date;
            }
        }

        //TODO Do we need child nodes?
        private LinkedHashMultimap<String, String> children;
        private LinkedHashMultimap<String, String> parents;
        private HashMap<String, Data> data;
        private LinkedHashMultimap<String, String> heads;
        private long fakeDate = System.currentTimeMillis();

        private Iterator<BitbucketChangesetPage> pages;
        private int pageNum = 0;

        public Graph()
        {
            initGraph();
        }

        private void initGraph()
        {
            children = LinkedHashMultimap.create();
            parents = LinkedHashMultimap.create();
            heads = LinkedHashMultimap.create();
            data = Maps.newHashMap();
        }

        public Graph merge(String node, String parentNode, String... parentNodes)
        {
            commit(node, data.get(parentNode).branch, parentNode, parentNodes);
            return this;
        }

        public Graph branch(String node, String parentNode)
        {
            branch(data.get(parentNode).branch, node, parentNode);
            return this;
        }

        public Graph branch(String newBranch, String node, String parentNode)
        {
            commit(node, newBranch, parentNode);
            return this;
        }

        public Graph commit(String node, String parentNode)
        {
            if (parentNode == null)
            {
                commit(node, "default", null);
            }
            else
            {
                commit(node, data.get(parentNode).branch, parentNode);
            }
            return this;
        }

        public Graph commit(String node, String branch, String parentNode, String... parentNodes)
        {
            if (parentNode != null)
            {
                addNode(node, branch, parentNode);
            }
            if (parentNodes != null)
            {
                for (String parent : parentNodes)
                {
                    addNode(node, branch, parent);
                }
            }

            data.put(node, new Data(branch, new Date(fakeDate)));
            fakeDate += 1000 * 60 * 60;

            heads.put(branch, node);
            return this;
        }

        private void addNode(String node, String branch, String parentNode)
        {
            parents.put(node, parentNode);
            children.put(parentNode, node);
            removeOldHead(branch, parentNode);
        }

        private void removeOldHead(String branch, String parentNode)
        {
            if (data.get(parentNode).branch.equals(branch))
            {
                heads.remove(branch, parentNode);
            }
        }

        public Set<String> getParent(String node)
        {
            return parents.get(node);
        }

        public List<String> getHeads()
        {
            return Lists.newArrayList(heads.values());
        }

        public List<String> getNodes()
        {
            return Lists.newArrayList(data.keySet());
        }

        public void mock()
        {
            Mockito.reset(bitbucketBranchesAndTags, branchesAndTagsRemoteRestpoint, changesetRestpoint, changesetService, commitService);

            when(bitbucketBranchesAndTags.getBranches()).thenReturn(
                    Lists.transform(Lists.newArrayList(heads.keySet()), new Function<String, BitbucketBranch>()
                    {
                        @Override
                        public BitbucketBranch apply(String input)
                        {
                            return bitbucketBranch(input);
                        }
                    }));
            when(branchesAndTagsRemoteRestpoint.getBranchesAndTags(anyString(), anyString())).thenReturn(bitbucketBranchesAndTags);
            try
            {
                when(egitRepositoryService.getBranches(any(IRepositoryIdProvider.class))).thenReturn(
                        Lists.transform(Lists.newArrayList(heads.keySet()), new Function<String, RepositoryBranch>()
                        {
                            @Override
                            public RepositoryBranch apply(@Nullable final String input)
                            {
                                return githubBranch(input);
                            }
                        })
                );
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }

            when(changesetRestpoint.getNextChangesetsPage(anyString(), anyString(), Mockito.anyListOf(String.class), Mockito.anyListOf(String.class), anyInt(), any(BitbucketChangesetPage.class))).then(new Answer<BitbucketChangesetPage>()
            {
                @Override
                public BitbucketChangesetPage answer(InvocationOnMock invocation) throws Throwable
                {
                    @SuppressWarnings ("unchecked")
                    BitbucketChangesetPage currentPage = (BitbucketChangesetPage) invocation.getArguments()[5];
                    @SuppressWarnings ("unchecked")
                    List<String> includes = (List<String>) invocation.getArguments()[2];
                    @SuppressWarnings ("unchecked")
                    List<String> excludes = (List<String>) invocation.getArguments()[3];
                    if (currentPage == null || StringUtils.isBlank(currentPage.getNext()))
                    {
                        pages = getPages(includes, excludes, BitbucketCommunicator.CHANGESET_LIMIT);
                        for (int i = 1; currentPage == null || i < currentPage.getPage(); i++)
                        {
                            pages.next();
                        }
                    }
                    return pages.next();
                }
            });

            when(changesetRestpoint.getChangeset(anyString(), anyString(), anyString())).then(new Answer<BitbucketChangeset>()
            {

                @Override
                public BitbucketChangeset answer(InvocationOnMock invocation) throws Throwable
                {
                    String node = (String) invocation.getArguments()[2];

                    BitbucketChangeset changeset = new BitbucketChangeset();
                    changeset.setNode(node);
                    changeset.setParents(new ArrayList<String>(parents.get(node)));
                    changeset.setFiles(Collections.<BitbucketChangesetFile>emptyList());

                    Data changesetData = data.get(node);
                    changeset.setBranch(changesetData.branch);
                    changeset.setUtctimestamp(changesetData.date);
                    return changeset;
                }

            });

            try
            {
                when(commitService.getCommit(any(RepositoryId.class), anyString())).then(new Answer<RepositoryCommit>()
                {
                    @Override
                    public RepositoryCommit answer(final InvocationOnMock invocation) throws Throwable
                    {
                        String sha = (String) invocation.getArguments()[1];

                        RepositoryCommit repositoryCommit = new RepositoryCommit();
                        Commit commit = new Commit();
                        repositoryCommit.setCommit(commit);
                        repositoryCommit.setSha(sha);
                        commit.setSha(sha);
                        repositoryCommit.setFiles(Collections.<CommitFile>emptyList());

                        List<Commit> parentsList = new ArrayList<Commit>();
                        for (String parent : parents.get(sha))
                        {
                            Commit parentCommit = new Commit();
                            parentCommit.setSha(parent);
                            parentsList.add(parentCommit);
                        }
                        repositoryCommit.setParents(parentsList);
                        commit.setParents(parentsList);

                        Data changesetData = data.get(sha);
                        CommitUser author = new CommitUser();
                        author.setDate(changesetData.date);
                        commit.setAuthor(author);
                        return repositoryCommit;
                    }
                });
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }

        public Iterator<BitbucketChangesetPage> getPages(final List<String> includes, final List<String> excludes, final int pagelen)
        {
            final Iterator<BitbucketNewChangeset> changesetIterator = getIterable(includes, excludes).iterator();

            return new AbstractIterator<BitbucketChangesetPage>()
            {
                @Override
                protected BitbucketChangesetPage computeNext()
                {
                    if (!changesetIterator.hasNext())
                    {
                        endOfData();
                        return null;
                    }
                    pageNum++;
                    int changesetNum = 1;
                    BitbucketChangesetPage page = new BitbucketChangesetPage();
                    page.setPagelen(pagelen);
                    page.setPage(pageNum);
                    List<BitbucketNewChangeset> values = new ArrayList<BitbucketNewChangeset>();

                    while (changesetNum <= pagelen && changesetIterator.hasNext())
                    {
                        BitbucketNewChangeset changeset = changesetIterator.next();
                        values.add(changeset);
                        changesetNum++;
                    }

                    if (changesetIterator.hasNext())
                    {
                        page.setNext("/?pagelen=" + pagelen + "&page=" + (pageNum + 1) + "&ctx=" + (pageNum + 1));
                    }
                    page.setValues(values);
                    return page;
                }
            };
        }

        public Iterable<BitbucketNewChangeset> getIterable(final List<String> includes, final List<String> excludes)
        {
            return new Iterable<BitbucketNewChangeset>()
            {

                @Override
                public Iterator<BitbucketNewChangeset> iterator()
                {
                    return Iterators.transform(Graph.this.iterator(includes, excludes), new Function<String, BitbucketNewChangeset>()
                    {
                        @Override
                        public BitbucketNewChangeset apply(String input)
                        {
                            BitbucketNewChangeset changeset = new BitbucketNewChangeset();
                            changeset.setHash(input);

                            List<BitbucketNewChangeset> parentsList = new ArrayList<BitbucketNewChangeset>();
                            for (String parent : parents.get(input))
                            {
                                BitbucketNewChangeset parentChangeset = new BitbucketNewChangeset();
                                parentChangeset.setHash(parent);
                                parentsList.add(parentChangeset);
                            }
                            changeset.setParents(parentsList);

                            Data changesetData = data.get(input);
                            changeset.setBranch(changesetData.branch);
                            changeset.setDate(changesetData.date);
                            return changeset;
                        }

                    });
                }
            };
        }

        private Iterator<String> iterator(final Collection<String> include, final Collection<String> exclude)
        {
            return new AbstractIterator<String>()
            {

                private final Set<String> nextNodes = Sets.newHashSet();
                private final Set<String> processedNodes = Sets.newHashSet();
                private final Set<String> excludeNodes = Sets.newHashSet();

                {
                    excludeNodes(exclude);

                    if (include == null || include.isEmpty())
                    {
                        includeNodes(heads.values());
                    }
                    else
                    {
                        includeNodes(include);
                    }
                }

                private void includeNodes(Collection<String> nodes)
                {
                    for (String node : nodes)
                    {
                        if (!excludeNodes.contains(node))
                        {
                            nextNodes.add(node);
                        }
                    }
                }

                private void excludeNodes(Collection<String> nodes)
                {
                    if (nodes == null)
                    {
                        return;
                    }

                    excludeNodes.addAll(nodes);

                    for (String node : nodes)
                    {
                        excludeNodes(getParent(node));
                    }
                }

                @Override
                protected String computeNext()
                {
                    if (nextNodes.isEmpty())
                    {
                        endOfData();
                        return null;
                    }

                    Iterator<String> it = nextNodes.iterator();
                    String node = it.next();
                    it.remove();
                    processedNodes.add(node);
                    addNodes(getParent(node));
                    return node;
                }

                private void addNodes(Set<String> nodes)
                {
                    for (String node : nodes)
                    {
                        if (!processedNodes.contains(node) && !excludeNodes.contains(node))
                        {
                            nextNodes.add(node);
                        }
                    }
                }
            };
        }

        private BitbucketBranch bitbucketBranch(String name)
        {
            BitbucketBranch bitbucketBranch = new BitbucketBranch();
            bitbucketBranch.setName(name);
            Set<String> head = heads.get(name);
            bitbucketBranch.setHeads(Lists.newArrayList(head));
            bitbucketBranch.setChangeset(head.iterator().next());

            return bitbucketBranch;
        }

        private RepositoryBranch githubBranch(String name)
        {
            RepositoryBranch gitHubBranch = new RepositoryBranch();
            gitHubBranch.setName(name);
            String head = heads.get(name).iterator().next();
            TypedResource commit = new TypedResource();
            commit.setSha(head);
            commit.setType(TypedResource.TYPE_COMMIT);
            gitHubBranch.setCommit(commit);
            return gitHubBranch;
        }

    }

    @Test
    public void getChangesets_Bitbucket_softSync()
    {
//       B3   D  B1   B2
//                    14
//            16 13   |
//             | |    12
//            10 11  /
//           / |/| >9
//          /  8 7
//         / / |/
//       15 4  6
//        \ |  |
//          3  5
//           \ |
//             2
//             |
//             1
        Graph graph = new Graph();
        final List<String> processedNodes = Lists.newArrayList();

        when(repositoryMock.getDvcsType()).thenReturn(BitbucketCommunicator.BITBUCKET);

        graph
                .commit("node1", null)
                .commit("node2", "node1")
                .branch("node3", "node2")
                .commit("node4", "node3")
                .commit("node5", "node2")
                .commit("node6", "node5")
                .merge("node8", "node6", "node4")
                .branch("B1", "node7", "node6")
                .branch("node9", "node7")
                .merge("node11", "node7", "node8", "node9")
                .commit("node13", "node11")
                .mock();

        checkSynchronization(graph, processedNodes, true);
        checkSynchronization(graph, processedNodes, true);

        // add more commits
        graph
                .branch("B2", "node12", "node9")
                .commit("node14", "node12")
                .branch("B3", "node15", "node3")
                .merge("node10", "node8", "node15")
                .commit("node16", "node10")
                .mock();

        checkSynchronization(graph, processedNodes, true);
        checkSynchronization(graph, processedNodes, true);
    }

    @Test
    public void getChangesets_Bitbucket_fullSync()
    {
        when(repositoryMock.getDvcsType()).thenReturn(BitbucketCommunicator.BITBUCKET);

        Graph graph = new Graph();

        graph
                .commit("node1", null)
                .commit("node2", "node1")
                .branch("node3", "node2")
                .commit("node4", "node3")
                .commit("node5", "node2")
                .commit("node6", "node5")
                .merge("node8", "node6", "node4")
                .branch("B1", "node7", "node6")
                .branch("node9", "node7")
                .merge("node11", "node7", "node8", "node9")
                .commit("node13", "node11")
                .mock();

        checkSynchronization(graph, false);

        // add more commits
        graph
                .branch("B2", "node12", "node9")
                .commit("node14", "node12")
                .branch("B3", "node15", "node3")
                .merge("node10", "node8", "node15")
                .commit("node16", "node10")
                .mock();

        checkSynchronization(graph, false);
    }

    @Test
    public void getChangesets_Bitbucket_softSyncWithNoHeads()
    {
        Graph graph = new Graph();
        final List<String> processedNodes = Lists.newArrayList();

        when(repositoryMock.getDvcsType()).thenReturn(BitbucketCommunicator.BITBUCKET);
        when(changesetDao.getChangesetCount(repositoryMock.getId())).thenReturn(1);

        graph
                .commit("node1", null)
                .commit("node2", "node1")
                .branch("node3", "node2")
                .commit("node4", "node3")
                .commit("node5", "node2")
                .commit("node6", "node5")
                .merge("node8", "node6", "node4")
                .branch("B1", "node7", "node6")
                .branch("node9", "node7")
                .merge("node11", "node7", "node8", "node9")
                .commit("node13", "node11")
                .mock();

        checkSynchronization(graph, processedNodes, true);
        checkSynchronization(graph, processedNodes, true);

        // add more commits
        graph
                .branch("B2", "node12", "node9")
                .commit("node14", "node12")
                .branch("B3", "node15", "node3")
                .merge("node10", "node8", "node15")
                .commit("node16", "node10")
                .mock();

        checkSynchronization(graph, processedNodes, true);
        checkSynchronization(graph, processedNodes, true);
    }

    @Test
    public void getChangesets_Bitbucket_disabledSynchronization()
    {
        when(featureManager.isEnabled(SyncDisabledHelper.DISABLE_BITBUCKET_SYNCHRONIZATION_FEATURE)).thenReturn(true);
        when(repositoryMock.getDvcsType()).thenReturn(BitbucketCommunicator.BITBUCKET);

        Graph graph = new Graph();

        graph
                .commit("node1", null)
                .commit("node2", "node1")
                .mock();

        checkNoSynchronization(false);
    }

    @Test
    public void syncEventsShouldBeStoredDuringSoftSync()
    {
        Graph graph = new Graph();
        final List<String> processedNodes = Lists.newArrayList();

        when(repositoryMock.getDvcsType()).thenReturn(BitbucketCommunicator.BITBUCKET);
        when(changesetDao.getChangesetCount(repositoryMock.getId())).thenReturn(1);

        graph.commit("node1", null).mock();
        checkSynchronization(graph, processedNodes, true);

        // Should not capture events on first sync - it's a full sync
        verify(notCapturingRepoSync, times(2)).finish();

        graph.commit("node2", "node1").mock();
        checkSynchronization(graph, processedNodes, true);

        // Note: this is not a true unit test so we end up testing the responsibilities of both the DefaultSynchronizer
        // and the MessageExecutor below.

        // a this point both the DefaultSynchronizer and MessageExecutor should store events
        //InOrder inOrder = Mockito.inOrder(repoSyncForDefaultSync, repoSyncForMessageExecutor);
        //inOrder.verify(repoSyncForMessageExecutor).finish();
        //inOrder.verify(repoSyncForDefaultSync).finish();

        Mockito.verify(repoSyncForMessageExecutor).finish();
        Mockito.verify(repoSyncForDefaultSync).finish();
    }

    @Test
    public void syncEventsShouldNotBePublishedDuringFullSync()
    {
        Graph graph = new Graph();
        final List<String> processedNodes = Lists.newArrayList();

        when(repositoryMock.getDvcsType()).thenReturn(BitbucketCommunicator.BITBUCKET);
        when(changesetDao.getChangesetCount(repositoryMock.getId())).thenReturn(1);

        graph.commit("node1", null).mock();

        checkSynchronization(graph, processedNodes, false);
        verifyZeroInteractions(repoSyncForDefaultSync);
    }

    @Test
    public void getChangesets_GitHub_softSync()
    {
//       B3   D  B1   B2
//                    14
//            16 13   |
//             | |    12
//            10 11  /
//           / |/| >9
//          /  8 7
//         / / |/
//       15 4  6
//        \ |  |
//          3  5
//           \ |
//             2
//             |
//             1
        Graph graph = new Graph();
        final List<String> processedNodes = Lists.newArrayList();

        when(repositoryMock.getDvcsType()).thenReturn(GithubCommunicator.GITHUB);

        graph
                .commit("node1", null)
                .commit("node2", "node1")
                .branch("node3", "node2")
                .commit("node4", "node3")
                .commit("node5", "node2")
                .commit("node6", "node5")
                .merge("node8", "node6", "node4")
                .branch("B1", "node7", "node6")
                .branch("node9", "node7")
                .merge("node11", "node7", "node8", "node9")
                .commit("node13", "node11")
                .mock();

        checkSynchronization(graph, processedNodes, true);
        checkSynchronization(graph, processedNodes, true);

        // add more commits
        graph
                .branch("B2", "node12", "node9")
                .commit("node14", "node12")
                .branch("B3", "node15", "node3")
                .merge("node10", "node8", "node15")
                .commit("node16", "node10")
                .mock();

        checkSynchronization(graph, processedNodes, true);
        checkSynchronization(graph, processedNodes, true);
    }

    @Test
    public void getChangesets_GitHub_fullSync()
    {
        when(repositoryMock.getDvcsType()).thenReturn(GithubCommunicator.GITHUB);

        Graph graph = new Graph();

        graph
                .commit("node1", null)
                .commit("node2", "node1")
                .branch("node3", "node2")
                .commit("node4", "node3")
                .commit("node5", "node2")
                .commit("node6", "node5")
                .merge("node8", "node6", "node4")
                .branch("B1", "node7", "node6")
                .branch("node9", "node7")
                .merge("node11", "node7", "node8", "node9")
                .commit("node13", "node11")
                .mock();

        checkSynchronization(graph, false);

        // add more commits
        graph
                .branch("B2", "node12", "node9")
                .commit("node14", "node12")
                .branch("B3", "node15", "node3")
                .merge("node10", "node8", "node15")
                .commit("node16", "node10")
                .mock();

        checkSynchronization(graph, false);
    }

    @Test
    public void getChangesets_GitHub_disabledSynchronization()
    {
        when(featureManager.isEnabled(SyncDisabledHelper.DISABLE_GITHUB_SYNCHRONIZATION_FEATURE)).thenReturn(true);
        when(repositoryMock.getDvcsType()).thenReturn(GithubCommunicator.GITHUB);

        Graph graph = new Graph();

        graph
                .commit("node1", null)
                .commit("node2", "node1")
                .mock();

        checkNoSynchronization(false);
    }

    @Test
    public void shouldFallBackToFullSyncWhenNoBranchHeads()
    {
        when(repositoryMock.getDvcsType()).thenReturn(BitbucketCommunicator.BITBUCKET);

        BitbucketCommunicator communicatorMock = mock(BitbucketCommunicator.class);
        CachingCommunicator bitbucketCachingCommunicator = new CachingCommunicator(cacheManager);
        bitbucketCachingCommunicator.setDelegate(communicatorMock);
        when(dvcsCommunicatorProvider.getCommunicator(eq(BitbucketCommunicator.BITBUCKET))).thenReturn(bitbucketCachingCommunicator);

        branchDao.removeAllBranchesInRepository(repositoryMock.getId());

        EnumSet<SynchronizationFlag> flags = EnumSet.of(SynchronizationFlag.SYNC_CHANGESETS, SynchronizationFlag.SOFT_SYNC);
        defaultSynchronizer.doSync(repositoryMock, flags);

        verify(communicatorMock).startSynchronisation(repositoryMock, EnumSet.of(SynchronizationFlag.SYNC_CHANGESETS), syncAuditLogMock.getID());
    }

    private void checkSynchronization(Graph graph, boolean softSync)
    {
        checkSynchronization(graph, new ArrayList<String>(), softSync);
    }

    private void checkNoSynchronization(boolean softSync)
    {
        EnumSet<SynchronizationFlag> flags = EnumSet.of(SynchronizationFlag.SYNC_CHANGESETS);
        if (softSync)
        {
            // soft sync
            flags.add(SynchronizationFlag.SOFT_SYNC);
        }

        try
        {
            defaultSynchronizer.doSync(repositoryMock, flags);
        }
        catch (SourceControlException.SynchronizationDisabled e)
        {
            // ignoring to proceed verification
        }

        verifyNoMoreInteractions(changesetService);
        verifyNoMoreInteractions(branchesAndTagsRemoteRestpoint);
        verifyNoMoreInteractions(changesetRestpoint);
        verifyNoMoreInteractions(githubClientProvider);
        verifyNoMoreInteractions(commitService);
        verifyNoMoreInteractions(egitRepositoryService);
    }

    @SuppressWarnings ("unchecked")
    private void checkSynchronization(final Graph graph, final List<String> processedNodes, boolean softSync)
    {
        EnumSet<SynchronizationFlag> flags = EnumSet.of(SynchronizationFlag.SYNC_CHANGESETS);
        if (softSync)
        {
            // soft sync
            flags.add(SynchronizationFlag.SOFT_SYNC);
        }

        Mockito.reset(changesetService);
        when(changesetService.getByNode(eq(repositoryMock.getId()), anyString())).then(new Answer<Changeset>()
        {
            @Override
            public Changeset answer(final InvocationOnMock invocation) throws Throwable
            {
                @SuppressWarnings ("unchecked")
                String node = (String) invocation.getArguments()[1];
                if (processedNodes.contains(node))
                {
                    return new Changeset(repositoryMock.getId(), node, null, graph.data.get(node).date);
                }

                return null;
            }
        });

        when(changesetService.create(any(Changeset.class), anySet())).then(new Answer<Changeset>()
        {
            @Override
            public Changeset answer(final InvocationOnMock invocation) throws Throwable
            {
                @SuppressWarnings ("unchecked")
                Changeset changeset = (Changeset) invocation.getArguments()[0];

                assertThat(processedNodes).doesNotContain(changeset.getNode());
                processedNodes.add(changeset.getNode());
                return changeset;
            }
        });

        defaultSynchronizer.doSync(repositoryMock, flags);

        assertThat(((BranchDaoMock) branchDao).getHeads(repositoryMock.getId())).as("BranchHeads are incorrectly saved").containsAll(graph.getHeads()).doesNotHaveDuplicates().hasSameSizeAs(graph.getHeads());

        int retry = 0;
        while (messagingService.getQueuedCount(null) > 0 && retry < 5)
        {
            retry++;
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                throw new RuntimeException(e);
            }
        }

        assertThat(processedNodes).as("Incorrect synchronization").containsAll(graph.getNodes()).doesNotHaveDuplicates().hasSameSizeAs(graph.getNodes());
    }
}
