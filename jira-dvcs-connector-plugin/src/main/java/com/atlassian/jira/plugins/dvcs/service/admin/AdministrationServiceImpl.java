package com.atlassian.jira.plugins.dvcs.service.admin;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.IssueToMappingFunction;
import com.atlassian.jira.plugins.dvcs.event.DevSummaryChangedEvent;
import com.atlassian.jira.plugins.dvcs.event.EventService;
import com.atlassian.jira.util.concurrent.ThreadFactories;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Resource;

public class AdministrationServiceImpl implements AdministrationService
{
    private static final ThreadFactory THREAD_FACTORY =
            ThreadFactories.namedThreadFactory(AdministrationServiceImpl.class.getSimpleName());
    private static final Logger log = LoggerFactory.getLogger(AdministrationServiceImpl.class);

    @Resource
    private ChangesetDao changesetDao;

    @Resource
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Resource (name = "eventServiceImpl")
    private EventService eventService;

    @Resource
    private DevSummaryCachePrimingStatus status;

    final ThreadLocalDelegateExecutorFactory executorFactory;

    private final Executor executor;

    public AdministrationServiceImpl(final ThreadLocalDelegateExecutorFactory executorFactory)
    {
        this.executorFactory = executorFactory;
        executor = executorFactory.createExecutor(Executors.newSingleThreadExecutor(THREAD_FACTORY));
    }

    @Override
    public boolean primeDevSummaryCache()
    {
        final int totalIssueCount = changesetDao.getNumberOfDistinctIssueKeysToCommit();
        final int totalPrIssueCount = repositoryPullRequestDao.getNumberOfDistinctIssueKeysToPullRequests();
        if (status.startExclusively(totalIssueCount, totalPrIssueCount))
        {
            executor.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    startPriming();
                }
            });
            return true;
        }
        else
        {
            return false;
        }
    }

    @VisibleForTesting
    void startPriming()
    {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try
        {
            changesetDao.forEachIssueToCommitMapping(new IssueKeyPrimeCacheClosure(eventService, status));
            repositoryPullRequestDao.forEachIssueKeyToPullRequest(new PullRequestPrimeCacheClosure(eventService, status));
            stopWatch.stop();
            status.finished(stopWatch.toString());
        }
        catch (RuntimeException e)
        {
            stopWatch.stop();
            status.failed(e, stopWatch.toString());
            log.info("processing failed in this time " + stopWatch, e);
        }

        log.info("priming cache took this long {}", stopWatch);
    }

    @VisibleForTesting
    static abstract class PrimeCacheClosure implements IssueToMappingFunction
    {
        protected final EventService eventService;
        protected final DevSummaryCachePrimingStatus status;

        PrimeCacheClosure(final EventService eventService, final DevSummaryCachePrimingStatus status)
        {
            this.eventService = eventService;
            this.status = status;
        }

        @Override
        public boolean execute(final String dvcsType, final int repositoryId, final Set<String> issueKeys)
        {
            eventService.storeEvent(repositoryId, new DevSummaryChangedEvent(repositoryId, dvcsType, issueKeys), false);
            updateCount(issueKeys.size());
            eventService.dispatchEvents(repositoryId);
            return !status.isStopped();
        }

        public abstract void updateCount(int amount);
    }

    @VisibleForTesting
    static class IssueKeyPrimeCacheClosure extends PrimeCacheClosure
    {
        IssueKeyPrimeCacheClosure(final EventService eventService, final DevSummaryCachePrimingStatus status)
        {
            super(eventService, status);
        }

        @Override
        public void updateCount(final int amount)
        {
            status.completedIssueKeyBatch(amount);
        }
    }

    @VisibleForTesting
    static class PullRequestPrimeCacheClosure extends PrimeCacheClosure
    {
        PullRequestPrimeCacheClosure(final EventService eventService, final DevSummaryCachePrimingStatus status)
        {
            super(eventService, status);
        }

        @Override
        public void updateCount(final int amount)
        {
            status.completedPullRequestIssueKeyBatch(amount);
        }
    }

    @Override
    public DevSummaryCachePrimingStatus getPrimingStatus()
    {
        return status;
    }

    @Override
    public void stopPriming()
    {
        status.stopped();
    }
}
