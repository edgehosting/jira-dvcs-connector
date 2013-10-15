package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.listener.PostponeOndemandPrSyncListener;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageKey;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.CachingDvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketCommunicator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeActivityMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.BitbucketSynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.message.oldsync.OldBitbucketSynchronizeCsetMsg;
import com.atlassian.jira.plugins.dvcs.spi.github.message.SynchronizeChangesetMessage;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeActivityMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.BitbucketSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.GithubSynchronizeChangesetMessageConsumer;
import com.atlassian.jira.plugins.dvcs.sync.OldBitbucketSynchronizeCsetMsgConsumer;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.google.common.collect.MapMaker;

/**
 * Synchronization service
 */
public class DefaultSynchronizer implements Synchronizer, DisposableBean, InitializingBean
{
    private final Logger log = LoggerFactory.getLogger(DefaultSynchronizer.class);

    @Resource
    private MessagingService messagingService;

    @Resource
    private ChangesetService changesetService;

    @Resource
    private BranchService branchService;

    @Resource
    private ChangesetCache changesetCache;

    @Resource
    private DvcsCommunicatorProvider communicatorProvider;

    @Resource
    private RepositoryDao repositoryDao;

    @Resource
    private RepositoryActivityDao repositoryActivityDao;

    @Resource
    private PostponeOndemandPrSyncListener posponePrSyncHelper;


    public DefaultSynchronizer()
    {
        super();
    }

    @Override
    public void doSync(Repository repository, EnumSet<SynchronizationFlag> flags)
    {
        boolean softSync = flags.contains(SynchronizationFlag.SOFT_SYNC);

        if (skipSync(repository)) {
            return;
        }

        if (repository.isLinked())
        {
            if (!softSync)
            {
                // we are doing full sync, lets delete all existing changesets
                // also required as GHCommunicator.getChangesets() returns only changesets not already stored in database
                changesetService.removeAllInRepository(repository.getId());
                branchService.removeAllBranchHeadsInRepository(repository.getId());
                repository.setLastCommitDate(null);
                if (flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS))
                {
                    repositoryActivityDao.removeAll(repository);
                    repository.setActivityLastSync(null);
                }
                repositoryDao.save(repository);
            }

            startProgress(repository);
            if (repository.getDvcsType().equals(BitbucketCommunicator.BITBUCKET))
            {
                // sync csets
                BranchFilterInfo filterNodes = getFilterNodes(repository);
                processBitbucketSync(repository, softSync, filterNodes);
                updateBranchHeads(repository, filterNodes.newHeads, filterNodes.oldHeads);
                // sync pull requests
                if (flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS) && posponePrSyncHelper.isAfterPostponedTime())
                {
                    MessageKey<BitbucketSynchronizeActivityMessage> key = messagingService.get( //
                            BitbucketSynchronizeActivityMessage.class, //
                            BitbucketSynchronizeActivityMessageConsumer.KEY //
                            );
                    messagingService.publish(key, new BitbucketSynchronizeActivityMessage(repository, softSync), UUID.randomUUID().toString());
                }

            } else
            {
                Date synchronizationStartedAt = new Date();
                for (BranchHead branchHead : communicatorProvider.getCommunicator(repository.getDvcsType()).getBranches(repository))
                {
                    SynchronizeChangesetMessage message = new SynchronizeChangesetMessage(repository, //
                            branchHead.getName(), branchHead.getHead(), //
                            synchronizationStartedAt, //
                            null, softSync);
                    MessageKey<SynchronizeChangesetMessage> key = messagingService.get( //
                            SynchronizeChangesetMessage.class, //
                            GithubSynchronizeChangesetMessageConsumer.KEY //
                            );
                    messagingService.publish(key, message, UUID.randomUUID().toString());
                }

            }

        }
    }

    protected void startProgress(Repository repository)
    {
        DefaultProgress progress = new DefaultProgress();
        progress.start();
        putProgress(repository, progress);
    }

    private boolean skipSync(Repository repository)
    {
        Progress progress = getProgress(repository.getId());
        return progress != null && !progress.isFinished();
    }

    protected void processBitbucketSync(Repository repository, boolean softSync, BranchFilterInfo filterNodes)
    {
        List<BranchHead> newBranchHeads = filterNodes.newHeads;

        if (filterNodes.oldHeads.isEmpty() && !changesetCache.isEmpty(repository.getId()))
        {
            log.info("No previous branch heads were found, switching to old changeset synchronization for repository [{}].", repository.getId());
            Date synchronizationStartedAt = new Date();
            for (BranchHead branchHead : newBranchHeads)
            {
                OldBitbucketSynchronizeCsetMsg message = new OldBitbucketSynchronizeCsetMsg(repository, //
                        branchHead.getName(), branchHead.getHead(), //
                        synchronizationStartedAt, //
                        null, newBranchHeads, softSync);
                MessageKey<OldBitbucketSynchronizeCsetMsg> key = messagingService.get( //
                        OldBitbucketSynchronizeCsetMsg.class, //
                        OldBitbucketSynchronizeCsetMsgConsumer.KEY //
                        );
                messagingService.publish(key, message, UUID.randomUUID().toString());
            }
        } else
        {
            if (CollectionUtils.isEmpty(getInclude(filterNodes))) {
                log.debug("No new changesets detected for repository [{}].", repository.getSlug());
                return;
            }
            MessageKey<BitbucketSynchronizeChangesetMessage> key = messagingService.get(
                    BitbucketSynchronizeChangesetMessage.class,
                    BitbucketSynchronizeChangesetMessageConsumer.KEY
                    );
            Date synchronizationStartedAt = new Date();

            BitbucketSynchronizeChangesetMessage message = new BitbucketSynchronizeChangesetMessage(repository, synchronizationStartedAt,
                    (Progress) null, filterNodes.newHeads, filterNodes.oldHeadsHashes, 1, asNodeToBranches(filterNodes.newHeads), softSync);

            messagingService.publish(key, message, UUID.randomUUID().toString());
        }
    }

    private Collection<String> getInclude(BranchFilterInfo filterNodes)
    {
        List<String> newNodes = extractBranchHeads(filterNodes.newHeads);
        if (newNodes != null && filterNodes.oldHeadsHashes != null)
        {
            newNodes.removeAll(filterNodes.oldHeadsHashes);
        }
        return newNodes;
    }

    protected void updateBranchHeads(Repository repo, List<BranchHead> newBranchHeads, List<BranchHead> oldHeads)
    {
        branchService.updateBranchHeads(repo, newBranchHeads, oldHeads);
    }

    protected BranchFilterInfo getFilterNodes(Repository repository)
    {
        CachingDvcsCommunicator cachingCommunicator = (CachingDvcsCommunicator) communicatorProvider
                .getCommunicator(BitbucketCommunicator.BITBUCKET);
        BitbucketCommunicator communicator = (BitbucketCommunicator) cachingCommunicator.getDelegate();
        List<BranchHead> newBranches = communicator.getBranches(repository);
        List<BranchHead> oldBranches = communicator.getOldBranches(repository);

        List<String> exclude = extractBranchHeads(oldBranches);

        BranchFilterInfo filter = new BranchFilterInfo(newBranches, oldBranches, exclude);
        return filter;
    }

    private List<String> extractBranchHeads(List<BranchHead> branchHeads)
    {
        if (branchHeads == null)
        {
            return null;
        }
        List<String> result = new ArrayList<String>();
        for (BranchHead branchHead : branchHeads)
        {
            result.add(branchHead.getHead());
        }
        return result;
    }

    private Map<String, String> asNodeToBranches(List<BranchHead> list)
    {
        Map<String, String> changesetBranch = new HashMap<String, String>();
        for (BranchHead branchHead : list)
        {
            changesetBranch.put(branchHead.getHead(), branchHead.getName());
        }
        return changesetBranch;
    }


    @Override
    public void stopSynchronization(Repository repository)
    {
        Progress progress = SynchronizationProgessHolder.progressMap.get(repository.getId());
        if (progress != null)
        {
            progress.setShouldCancel(true);
        }
    }

    @Override
    public void pauseSynchronization(Repository repository, boolean pause)
    {
        Progress progress = SynchronizationProgessHolder.progressMap.get(repository.getId());
        if (progress != null)
        {
            // TODO
        }

    }

    @Override
    public Progress getProgress(int repositoryId)
    {
        return SynchronizationProgessHolder.getProgress(repositoryId);
    }

    @Override
    public void putProgress(Repository repository, Progress progress)
    {
        SynchronizationProgessHolder.progressMap.put(repository.getId(), progress);
    }

    @Override
    public void removeProgress(Repository repository)
    {
        SynchronizationProgessHolder.removeProgress(repository.getId());
    }

    @Override
    public void destroy() throws Exception
    {
        // TODO
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        // TODO
    }

    private static class BranchFilterInfo {

        private List<BranchHead> newHeads;
        private List<BranchHead> oldHeads;
        private List<String> oldHeadsHashes;

        public BranchFilterInfo(List<BranchHead> newHeads, List<BranchHead> oldHeads, List<String> oldHeadsHashes)
        {
            super();
            this.newHeads = newHeads;
            this.oldHeads = oldHeads;
            this.oldHeadsHashes = oldHeadsHashes;
        }
    }

    public static class SynchronizationProgessHolder
    {
        // map of ALL Synchronisation Progresses - running and finished ones
        static final ConcurrentMap<Integer, Progress> progressMap = new MapMaker().makeMap();

        public static void removeProgress(int id)
        {
            progressMap.remove(id);
        }

        public static Progress getProgress(int id)
        {
            return progressMap.get(id);
        }
    }

}
