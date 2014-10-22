package com.atlassian.jira.plugins.dvcs.sync.impl;

import com.atlassian.beehive.ClusterLockService;
import com.atlassian.beehive.compat.ClusterLockServiceFactory;
import com.atlassian.cache.Cache;
import com.atlassian.cache.CacheManager;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.analytics.DvcsSyncStartAnalyticsEvent;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.event.RepositorySync;
import com.atlassian.jira.plugins.dvcs.event.RepositorySyncHelper;
import com.atlassian.jira.plugins.dvcs.exception.SourceControlException;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.BranchService;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.service.remote.SyncDisabledHelper;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Date;
import java.util.EnumSet;
import java.util.concurrent.locks.Lock;
import javax.annotation.Resource;

/**
 * Synchronization service
 */
@Component
public class DefaultSynchronizer implements Synchronizer
{
    @VisibleForTesting
    static final String SYNC_LOCK = DefaultSynchronizer.class.getName() + ".doSync";

    private static final Logger LOG = LoggerFactory.getLogger(DefaultSynchronizer.class);

    @Resource
    private MessagingService messagingService;

    @Resource
    private ChangesetService changesetService;

    @Resource
    private BranchService branchService;

    @Resource
    private DvcsCommunicatorProvider communicatorProvider;

    @Resource
    private RepositoryDao repositoryDao;

    @Resource
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Resource
    private SyncAuditLogDao syncAudit;

    @Resource
    @ComponentImport
    private EventPublisher eventPublisher;

    @Resource
    private GitHubEventService gitHubEventService;

    @Resource
    private RepositorySyncHelper repoSyncHelper;

    @Resource
    private SyncDisabledHelper syncDisabledHelper;

    private final ClusterLockService clusterLockService;

    // Cache of all synchronisation progresses, both running and finished
    private final Cache<Integer, Progress> progressMap;

    @Autowired
    public DefaultSynchronizer(@ComponentImport final CacheManager cacheManager,
            final ClusterLockServiceFactory clusterLockServiceFactory)
    {
        this.clusterLockService = clusterLockServiceFactory.getClusterLockService();
        this.progressMap = cacheManager.getCache(getClass().getName() + ".progressMap");
        // clear the cache as a temp fix for BBC-744
        if (LOG.isDebugEnabled())
        {
            final Collection<Integer> keys = progressMap.getKeys();
            if (!keys.isEmpty())
            {
                LOG.debug("clearing progressMap of size {} ", keys.size());
            }
        }
        progressMap.removeAll();
    }


    @Override
    public void doSync(Repository repo, EnumSet<SynchronizationFlag> flagsOrig)
    {
        DvcsCommunicator communicator = communicatorProvider.getCommunicator(repo.getDvcsType());

        // We take a copy of the flags, so we can modify them as we want for this sync without others who reuse the flags being affected.
        EnumSet<SynchronizationFlag> flags = EnumSet.copyOf(flagsOrig);

        if (communicator.isSyncDisabled(repo, flags))
        {
            LOG.info("Synchronization is disabled for repository {} ({})", repo.getName(), repo.getId());
            throw new SourceControlException.SynchronizationDisabled("Synchronization is disabled for repository " + repo.getName() + " (" + repo.getId() + ")");
        }

        if (repo.isLinked() && !repo.isDeleted())
        {
            // Remove the soft sync flag if we have no branch heads.
            if (branchService.getListOfBranchHeads(repo).isEmpty())
            {
                flags.remove(SynchronizationFlag.SOFT_SYNC);
            }

            Progress progress = startProgressSafely(repo, flags);
            if (progress == null)
            {
                return;
            }

            boolean softSync = flags.contains(SynchronizationFlag.SOFT_SYNC);
            boolean changesetsSync = flags.contains(SynchronizationFlag.SYNC_CHANGESETS);
            boolean pullRequestSync = flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS);

            final RepositorySync repoSync = repoSyncHelper.startSync(repo, flags);
            fireAnalyticsStart(softSync, changesetsSync, pullRequestSync, flags.contains(SynchronizationFlag.WEBHOOK_SYNC));
            int auditId = 0;
            try
            {
                // audit
                auditId = syncAudit.newSyncAuditLog(repo.getId(), getSyncType(flags), new Date(progress.getStartTime())).getID();
                progress.setAuditLogId(auditId);

                if (!softSync)
                {
                    if (!syncDisabledHelper.isFullSychronizationDisabled())
                    {
                        removeRepoDataForFullSync(repo, flags);
                    }
                    else
                    {
                        LOG.info("Full synchronization is disabled. Doing a soft sync for repository {} ({}) instead.", repo.getName(), repo.getId());
                    }
                }

                // first retry all failed messages
                retryFailedMessages(repo, auditId);

                if (syncDisabledHelper.isPullRequestSynchronizationDisabled())
                {
                    flags.remove(SynchronizationFlag.SYNC_PULL_REQUESTS);
                }

                communicator.startSynchronisation(repo, flags, auditId);
            }
            catch (Throwable t)
            {
                LOG.error(t.getMessage(), t);
                progress.setError("Error during sync. See server logs.");
                syncAudit.setException(auditId, t, false);
                Throwables.propagateIfInstanceOf(t, Error.class);
            }
            finally
            {
                repoSync.finish();
                messagingService.tryEndProgress(repo, progress, null, auditId);
            }
        }
    }

    private Progress startProgressSafely(Repository repo, EnumSet<SynchronizationFlag> flags)
    {
        final Lock lock = clusterLockService.getLockForName(SYNC_LOCK);
        lock.lock();
        try
        {
            if (skipSync(repo, flags))
            {
                return null;
            }
            return startProgress(repo, flags);
        }
        finally
        {
            lock.unlock();
        }
    }

    private void removeRepoDataForFullSync(Repository repo, EnumSet<SynchronizationFlag> flags)
    {
        //TODO This will deleted both changeset and PR messages, we should distinguish between them
        // Stopping synchronization to delete failed messages for repository
        stopSynchronization(repo);
        if (flags.contains(SynchronizationFlag.SYNC_CHANGESETS))
        {
            // we are doing full sync, lets delete all existing changesets
            // also required as GHCommunicator.getChangesets() returns only changesets not already stored in database
            changesetService.removeAllInRepository(repo.getId());
            branchService.removeAllBranchHeadsInRepository(repo.getId());
            branchService.removeAllBranchesInRepository(repo.getId());

            repo.setLastCommitDate(null);
        }
        if (flags.contains(SynchronizationFlag.SYNC_PULL_REQUESTS))
        {
            gitHubEventService.removeAll(repo);
            repositoryPullRequestDao.removeAll(repo);
            repo.setActivityLastSync(null);
        }
        repositoryDao.save(repo);
    }

    private void retryFailedMessages(Repository repo, int auditId)
    {
        try
        {
            messagingService.retry(messagingService.getTagForSynchronization(repo), auditId);
        }
        catch (Exception e)
        {
            LOG.warn("Could not resume failed messages.", e);
        }

    }

    private void fireAnalyticsStart(boolean softSync, boolean changesetsSync, boolean pullRequestSync, boolean webhook)
    {
        eventPublisher.publish(new DvcsSyncStartAnalyticsEvent(softSync, changesetsSync, pullRequestSync, webhook));
    }

    private Progress startProgress(Repository repository, EnumSet<SynchronizationFlag> flags)
    {
        DefaultProgress progress = new DefaultProgress();
        progress.setSoftsync(flags.contains(SynchronizationFlag.SOFT_SYNC));
        progress.setWebHookSync(flags.contains(SynchronizationFlag.WEBHOOK_SYNC));
        progress.start();
        putProgress(repository, progress);
        return progress;
    }

    protected String getSyncType(final EnumSet<SynchronizationFlag> flags)
    {
        final StringBuilder bld = new StringBuilder();
        for (final SynchronizationFlag flag : flags)
        {
            switch (flag)
            {
                case SOFT_SYNC:
                    bld.append(SyncAuditLogMapping.SYNC_TYPE_SOFT).append(" ");
                    break;
                case SYNC_CHANGESETS:
                    bld.append(SyncAuditLogMapping.SYNC_TYPE_CHANGESETS).append(" ");
                    break;
                case SYNC_PULL_REQUESTS:
                    bld.append(SyncAuditLogMapping.SYNC_TYPE_PULLREQUESTS).append(" ");
                    break;
                case WEBHOOK_SYNC:
                    bld.append(SyncAuditLogMapping.SYNC_TYPE_WEBHOOKS).append(" ");
                    break;
                default: // Do nothing.
                    break;
            }
        }
        return bld.toString();
    }

    private boolean skipSync(Repository repository, EnumSet<SynchronizationFlag> flags)
    {
        Progress progress = getProgress(repository.getId());

        if (progress == null || progress.isFinished())
        {
            return false;
        }

        if (flags.contains(SynchronizationFlag.WEBHOOK_SYNC))
        {
            LOG.info("Postponing post webhook synchronization. It will start after the running synchronization finishes.");

            EnumSet<SynchronizationFlag> currentFlags = progress.getRunAgainFlags();
            if (currentFlags == null)
            {
                progress.setRunAgainFlags(flags);
            }
            else
            {
                currentFlags.addAll(flags);
            }
        }
        return true;
    }

    @Override
    public void stopSynchronization(Repository repository)
    {
        messagingService.cancel(messagingService.getTagForSynchronization(repository));
    }

    @Override
    public void pauseSynchronization(Repository repository, boolean pause)
    {
        if (pause)
        {
            messagingService.pause(messagingService.getTagForSynchronization(repository));
        }
        else
        {
            messagingService.resume(messagingService.getTagForSynchronization(repository));
        }
    }

    @Override
    public Progress getProgress(int repositoryId)
    {
        return progressMap.get(repositoryId);
    }

    @Override
    @SuppressWarnings ("deprecation")    // put is un-deprecated in a later version of atlassian-cache
    public void putProgress(Repository repository, Progress progress)
    {
        progressMap.put(repository.getId(), progress);
    }

    @Override
    public void removeProgress(Repository repository)
    {
        progressMap.remove(repository.getId());
    }

    @Override
    public void removeAllProgress()
    {
        progressMap.removeAll();
    }
}
