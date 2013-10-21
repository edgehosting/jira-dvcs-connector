package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.annotation.Resource;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.listener.PostponeOndemandPrSyncListener;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetCache;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
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

    @Resource
    private SyncAuditLogDao syncAudit;


    public DefaultSynchronizer()
    {
        super();
    }

    @Override
    public void doSync(Repository repo, EnumSet<SynchronizationFlag> flags)
    {
        boolean softSync = flags.contains(SynchronizationFlag.SOFT_SYNC);
        boolean changestesSync = flags.contains(SynchronizationFlag.SYNC_CHANGESETS);
        boolean pullRequestSync = flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS);
        int auditId = 0;

        if (skipSync(repo)) {
            return;
        }

        if (repo.isLinked())
        {
            if (!softSync)
            {
                //TODO This will deleted both changeset and PR messages, we should distinguish between them
                // Stopping synchronization to delete failed messages for repository
                stopSynchronization(repo);
                if (changestesSync)
                {
                    // we are doing full sync, lets delete all existing changesets
                    // also required as GHCommunicator.getChangesets() returns only changesets not already stored in database
                    changesetService.removeAllInRepository(repo.getId());
                    branchService.removeAllBranchHeadsInRepository(repo.getId());
                    repo.setLastCommitDate(null);
                }
                if (pullRequestSync)
                {
                    repositoryActivityDao.removeAll(repo);
                    repo.setActivityLastSync(null);
                }
                repositoryDao.save(repo);
            }

            try
            {
                auditId = startProgress(repo, softSync);

                // first retry all failed messages
                messagingService.retry(messagingService.getTagForSynchronization(repo));

                try
                {
                    messagingService.retry(messagingService.getTagForSynchronization(repo));
                } catch (Exception e)
                {
                    log.warn("Could not resume failed messages.", e);
                }

                if (repo.getDvcsType().equals(BitbucketCommunicator.BITBUCKET))
                {
                    if (changestesSync)
                    {
                        // sync csets
                        BranchFilterInfo filterNodes = getFilterNodes(repo);
                        processBitbucketCsetSync(repo, softSync, filterNodes, auditId);
                        updateBranchHeads(repo, filterNodes.newHeads, filterNodes.oldHeads);
                    }
                    // sync pull requests
                    if (pullRequestSync && posponePrSyncHelper.isAfterPostponedTime())
                    {
                        processBitbucketPrSync(repo, softSync, auditId);
                    }

                } else
                {
                    if (changestesSync)
                    {
                        Date synchronizationStartedAt = new Date();
                        for (BranchHead branchHead : communicatorProvider.getCommunicator(repo.getDvcsType()).getBranches(repo))
                        {
                            SynchronizeChangesetMessage message = new SynchronizeChangesetMessage(repo, //
                                    branchHead.getName(), branchHead.getHead(), //
                                    synchronizationStartedAt, //
                                    null, softSync, auditId);
                            MessageAddress<SynchronizeChangesetMessage> key = messagingService.get( //
                                    SynchronizeChangesetMessage.class, //
                                    GithubSynchronizeChangesetMessageConsumer.KEY //
                                    );
                            messagingService.publish(key, message, messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId));
                        }
                    }
                    if (pullRequestSync)
                    {
                        // TODO
                    }
                }
            } catch (Exception e)
            {
                log.error(e.getMessage(), e);
                Progress progress = getProgress(repo.getId());
                progress.setError("Error during sync. See server logs.");
            } finally
            {
                messagingService.tryEndProgress(repo, getProgress(repo.getId()), null, auditId);
            }
        }
    }

    private int startProgress(Repository repository, boolean softSync)
    {
        DefaultProgress progress = new DefaultProgress();
        progress.start();
        putProgress(repository, progress);
        // audit
        int auditId = syncAudit.newSyncAuditLog(repository.getId(), getSyncType(softSync)).getID();
        progress.setAuditLogId(auditId);
        return auditId;
    }

    protected String getSyncType(boolean softSync)
    {
        return softSync ? SyncAuditLogMapping.SYNC_TYPE_SOFT : SyncAuditLogMapping.SYNC_TYPE_FULL;
    }

    private boolean skipSync(Repository repository)
    {
        Progress progress = getProgress(repository.getId());
        return progress != null && !progress.isFinished();
    }

    private void processBitbucketCsetSync(Repository repository, boolean softSync, BranchFilterInfo filterNodes, int auditId)
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
                        null, newBranchHeads, softSync, auditId);
                MessageAddress<OldBitbucketSynchronizeCsetMsg> key = messagingService.get( //
                        OldBitbucketSynchronizeCsetMsg.class, //
                        OldBitbucketSynchronizeCsetMsgConsumer.KEY //
                        );
                messagingService.publish(key, message, messagingService.getTagForSynchronization(repository), messagingService.getTagForAuditSynchronization(auditId));
            }
        } else
        {
            if (CollectionUtils.isEmpty(getInclude(filterNodes))) {
                log.debug("No new changesets detected for repository [{}].", repository.getSlug());
                return;
            }
            MessageAddress<BitbucketSynchronizeChangesetMessage> key = messagingService.get(
                    BitbucketSynchronizeChangesetMessage.class,
                    BitbucketSynchronizeChangesetMessageConsumer.KEY
                    );
            Date synchronizationStartedAt = new Date();

            BitbucketSynchronizeChangesetMessage message = new BitbucketSynchronizeChangesetMessage(repository, synchronizationStartedAt,
                    (Progress) null, filterNodes.newHeads, filterNodes.oldHeadsHashes, 1, asNodeToBranches(filterNodes.newHeads), softSync, auditId);

            messagingService.publish(key, message, messagingService.getTagForSynchronization(repository), messagingService.getTagForAuditSynchronization(auditId));
        }
    }

    protected void processBitbucketPrSync(Repository repo, boolean softSync, int auditId)
    {
        MessageAddress<BitbucketSynchronizeActivityMessage> key = messagingService.get( //
                BitbucketSynchronizeActivityMessage.class, //
                BitbucketSynchronizeActivityMessageConsumer.KEY //
                );
        messagingService.publish(key, new BitbucketSynchronizeActivityMessage(repo, softSync, repo.getActivityLastSync(), auditId),
                messagingService.getTagForSynchronization(repo), messagingService.getTagForAuditSynchronization(auditId));
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

    private void updateBranchHeads(Repository repo, List<BranchHead> newBranchHeads, List<BranchHead> oldHeads)
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
        messagingService.cancel(messagingService.getTagForSynchronization(repository));
    }

    @Override
    public void pauseSynchronization(Repository repository, boolean pause)
    {
        if (pause) {
            messagingService.pause(messagingService.getTagForSynchronization(repository));
        } else {
            messagingService.resume(messagingService.getTagForSynchronization(repository));
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
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
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
