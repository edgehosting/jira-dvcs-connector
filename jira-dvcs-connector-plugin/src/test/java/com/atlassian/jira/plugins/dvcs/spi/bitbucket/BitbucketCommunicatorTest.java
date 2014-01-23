package com.atlassian.jira.plugins.dvcs.spi.bitbucket;

import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.model.Branch;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.BranchServiceImpl;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.LinkedIssueService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranch;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranchesAndTags;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetFile;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketChangesetPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketNewChangeset;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.BranchesAndTagsRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.ChangesetRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker.BitbucketLinker;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginInformation;
import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Iterators;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import junit.framework.Assert;
import org.mockito.ArgumentCaptor;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySetOf;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class BitbucketCommunicatorTest
{
    @Mock
    private Repository repositoryMock;
    @Mock
    private BitbucketLinker bitbucketLinker;
    @Mock
    private PluginAccessor pluginAccessor;

    private BitbucketClientBuilder bitbucketClientBuilder;

    @Mock
    private BitbucketClientBuilderFactory bitbucketClientBuilderFactory;

    @Mock
    private ChangesetCache changesetCache;

    private BranchService branchService;

    @Mock
    private MessagingService messagingService;

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

    private CachingCommunicator communicator;

    private BitbucketCommunicator bitbucketCommunicator;

    @Mock
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    private BitbucketSynchronizeChangesetMessageConsumer consumer;

    @Mock
    private ChangesetService changesetService;

    @Mock
    private RepositoryService repositoryService;

    @Mock
    private LinkedIssueService linkedIssueService;

    private static class BuilderAnswer implements Answer<Object>
    {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable
        {
            Object builderMock = invocation.getMock();
            if (invocation.getMethod().getReturnType().isInstance(builderMock))
            {
                return builderMock;
            } else
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
            } else
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
    public void initializeMocksAndGithubCommunicator()
    {
        MockitoAnnotations.initMocks(this);

        when(pluginInformation.getVersion()).thenReturn("0");
        when(plugin.getPluginInformation()).thenReturn(pluginInformation);
        when(pluginAccessor.getPlugin(anyString())).thenReturn(plugin);

        branchDao = new BranchDaoMock();
        branchService = new BranchServiceImpl();
        ReflectionTestUtils.setField(branchService, "branchDao", branchDao);
        ReflectionTestUtils.setField(branchService, "dvcsCommunicatorProvider", dvcsCommunicatorProvider);

        bitbucketClientBuilder = mock(BitbucketClientBuilder.class, new BuilderAnswer());

        when(bitbucketClientBuilderFactory.forRepository(Matchers.any(Repository.class))).thenReturn(bitbucketClientBuilder);

        communicator = new CachingCommunicator();
        bitbucketCommunicator = new BitbucketCommunicator(bitbucketLinker, pluginAccessor, bitbucketClientBuilderFactory, changesetCache);
        ReflectionTestUtils.setField(bitbucketCommunicator, "branchService", branchService);
        ReflectionTestUtils.setField(bitbucketCommunicator, "messagingService", messagingService);

        communicator.setDelegate(bitbucketCommunicator);

        when(bitbucketClientBuilder.build()).thenReturn(bitbucketRemoteClient);
        when(bitbucketRemoteClient.getChangesetsRest()).thenReturn(changesetRestpoint);
        when(bitbucketRemoteClient.getBranchesAndTagsRemoteRestpoint()).thenReturn(branchesAndTagsRemoteRestpoint);

        consumer = new BitbucketSynchronizeChangesetMessageConsumer();
        ReflectionTestUtils.setField(consumer, "cachingCommunicator", communicator);
        ReflectionTestUtils.setField(consumer, "messagingService", messagingService);
        ReflectionTestUtils.setField(consumer, "changesetService", changesetService);
        ReflectionTestUtils.setField(consumer, "linkedIssueService", linkedIssueService);
        ReflectionTestUtils.setField(consumer, "repositoryService", repositoryService);

    }

    private class Graph
    {
        private class Data
        {
            private final String node;
            private final String branch;
            private final Date date;

            Data(String node, String branch, Date date)
            {
                this.node = node;
                this.branch = branch;
                this.date = date;
            }
        }

        //TODO Do we need child nodes?
        private LinkedHashMultimap<String, String> children;
        private LinkedHashMultimap<String, String> parents;
        private HashMap<String, Data> data;
        private LinkedHashMultimap<String,String> heads;
        private long fakeDate = System.currentTimeMillis();

        private Iterator<BitbucketChangesetPage> pages;
        private int pageNum = 0;
        private  BitbucketChangesetPage page;
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
            } else
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

            data.put(node, new Data(node, branch, new Date(fakeDate)));
            fakeDate += 1000*60*60;

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

        public Set<String> getChildren(String node)
        {
            return children.get(node);
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
            Mockito.reset(bitbucketBranchesAndTags, branchesAndTagsRemoteRestpoint, changesetRestpoint, changesetService, messagingService);

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

            when(changesetRestpoint.getNextChangesetsPage(anyString(), anyString(), Mockito.anyListOf(String.class), Mockito.anyListOf(String.class), anyInt(), any(BitbucketChangesetPage.class))).then(new Answer<BitbucketChangesetPage>()
            {
                @Override
                public BitbucketChangesetPage answer(InvocationOnMock invocation) throws Throwable
                {
                    @SuppressWarnings("unchecked")
                    BitbucketChangesetPage page = (BitbucketChangesetPage) invocation.getArguments()[5];
                    @SuppressWarnings("unchecked")
                    int pageLen = (Integer) invocation.getArguments()[4];
                    @SuppressWarnings("unchecked")
                    List<String> includes = (List<String>) invocation.getArguments()[2];
                    @SuppressWarnings("unchecked")
                    List<String> excludes = (List<String>) invocation.getArguments()[3];

                    return pages.next();
                }
            });

            when(changesetRestpoint.getChangeset(anyString(), anyString(), anyString())).then(new Answer<BitbucketChangeset>()
            {

                @Override
                public BitbucketChangeset answer(InvocationOnMock invocation) throws Throwable
                {
                    String node = (String)invocation.getArguments()[2];

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
        }

        public Iterable<BitbucketSynchronizeChangesetMessage> generateMessages(final List<String> includes, final List<String> excludes, final boolean softSync)
        {
            pages =  getPages(includes, excludes, BitbucketCommunicator.CHANGESET_LIMIT);
            pageNum = 0;
            page = null;
            return new Iterable<BitbucketSynchronizeChangesetMessage>()
            {
                @Override
                public Iterator<BitbucketSynchronizeChangesetMessage> iterator()
                {
                    return new AbstractIterator<BitbucketSynchronizeChangesetMessage>()
                    {
                        private Progress progressMock = Mockito.mock(DefaultProgress.class);
                        private HashMap<String, String> nodesToBranches = new HashMap<String, String>();

                        @Override
                        protected BitbucketSynchronizeChangesetMessage computeNext()
                        {
                            if (pages.hasNext())
                            {
                                BitbucketSynchronizeChangesetMessage message = new BitbucketSynchronizeChangesetMessage(repositoryMock,
                                        null, progressMock, includes, excludes, page, nodesToBranches, softSync, 0);
                                return message;
                            } else
                            {
                                endOfData();
                                return null;
                            }
                        }
                    };
                }
            };
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
                        System.out.println("Changeset " + changeset.getHash());
                        values.add(changeset);
                        changesetNum++;
                    }

                    if (changesetIterator.hasNext())
                    {
                        page.setNext("/?pagelen="+pagelen+"&page="+(pageNum+1)+"&ctx="+(pageNum+1));
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
                            changeset.setParents(Collections.<BitbucketNewChangeset>emptyList());

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
            Iterator<String> iterator = new AbstractIterator<String>()
            {

                private final Set<String> nextNodes = Sets.newHashSet();
                private final Set<String> processedNodes = Sets.newHashSet();
                private final Set<String> excludeNodes = Sets.newHashSet();

                {
                    excludeNodes(exclude);

                    if (include == null || include.isEmpty())
                    {
                        includeNodes(heads.values());
                    } else
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
            return iterator;
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

    }

    @Test
    public void getChangesets_softSync()
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

        when(changesetCache.isEmpty(anyInt())).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                return processedNodes.isEmpty();
            }
        });

        when(changesetCache.isCached(anyInt(), anyString())).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                String node = (String)invocation.getArguments()[1];

                return processedNodes.contains(node);
            }
        });

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
    public void getChangesets_fullSync()
    {
        when(changesetCache.isEmpty(anyInt())).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                return true;
            }
        });

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
    public void getChangesets_softSyncWithNoHeads()
    {
        Graph graph = new Graph();
        final List<String> processedNodes = Lists.newArrayList();

        when(changesetCache.isEmpty(anyInt())).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                return false;
            }
        });

        when(changesetCache.isCached(anyInt(), anyString())).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                String node = (String)invocation.getArguments()[1];

                return processedNodes.contains(node);
            }
        });

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

    private void checkSynchronization(Graph graph, boolean softSync)
    {
        checkSynchronization(graph, new ArrayList<String>(), softSync);
    }

    private void checkSynchronization(Graph graph, List<String> processedNodes, boolean softSync)
    {
        EnumSet<SynchronizationFlag> flags = EnumSet.of(SynchronizationFlag.SYNC_CHANGESETS);
        if (!softSync)
        {
            branchService.removeAllBranchHeadsInRepository(repositoryMock.getId());
        } else
        {
            // soft sync
            flags.add(SynchronizationFlag.SOFT_SYNC);
        }

        List<String> oldHeads = new ArrayList(((BranchDaoMock) branchDao).getHeads(repositoryMock.getId()));
        communicator.startSynchronisation(repositoryMock, flags, 0);
        List<String> newHeads = new ArrayList(((BranchDaoMock) branchDao).getHeads(repositoryMock.getId()));

        assertThat(((BranchDaoMock)branchDao).getHeads(repositoryMock.getId())).as("BranchHeads are incorrectly saved").containsAll(graph.getHeads()).doesNotHaveDuplicates().hasSameSizeAs(graph.getHeads());

        ArgumentCaptor<BitbucketSynchronizeChangesetMessage> messageCaptor = ArgumentCaptor.forClass(BitbucketSynchronizeChangesetMessage.class);
        verify(messagingService).publish(any(MessageAddress.class), messageCaptor.capture(), anyInt(), Mockito.<String>anyVararg());

        if (!oldHeads.containsAll(newHeads) || !newHeads.containsAll(oldHeads))
        {
            BitbucketSynchronizeChangesetMessage firstMessage = messageCaptor.getValue();

            List<String> includes = firstMessage.getInclude();
            List<String> excludes = firstMessage.getExclude();
            List<String> includeExpected = new ArrayList<String>(newHeads);
            includeExpected.removeAll(oldHeads);
            assertThat(includes).as("Includes are incorrect").containsAll(includeExpected).doesNotHaveDuplicates().hasSameSizeAs(includeExpected);
            assertThat(excludes).as("Excludes are incorrect").containsAll(oldHeads).doesNotHaveDuplicates().hasSameSizeAs(oldHeads);

            System.out.println("includes=" + includes);
            System.out.println("excludes=" + excludes);

            for (BitbucketSynchronizeChangesetMessage message : graph.generateMessages(includes, excludes, softSync))
            {
               System.out.println(message.getPage());
               consumer.onReceive(new Message<BitbucketSynchronizeChangesetMessage>(), message);

            }

            ArgumentCaptor<Changeset> savedChangesetCaptor = ArgumentCaptor.forClass(Changeset.class);
            verify(changesetService, atMost(graph.getNodes().size())).create(savedChangesetCaptor.capture(), anySetOf(String.class));

            for ( Changeset  changeset : savedChangesetCaptor.getAllValues())
            {
                assertThat(processedNodes).doesNotContain(changeset.getNode());
                processedNodes.add(changeset.getNode());
            }
        }
        assertThat(processedNodes).as("Incorrect synchronization").containsAll(graph.getNodes()).doesNotHaveDuplicates().hasSameSizeAs(graph.getNodes());
    }
}
