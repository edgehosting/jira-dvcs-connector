package com.atlassian.jira.plugins.dvcs.service.admin;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.IssueToMappingClosure;
import com.atlassian.jira.plugins.dvcs.event.DevSummaryChangedEvent;
import com.atlassian.jira.plugins.dvcs.event.EventService;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import javax.annotation.Resource;

public class AdministrationServiceImpl implements AdministrationService
{

    private static final Logger log = LoggerFactory.getLogger(AdministrationServiceImpl.class);

    @Resource
    private ChangesetDao changesetDao;

    @Resource
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Resource (name = "eventServiceImpl")
    private EventService eventService;

    @Resource
    private DevSummaryCachePrimingStatus status;

    @Override
    public boolean primeDevSummaryCache()
    {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final int totalIssueCount = changesetDao.getNumberOfDistinctIssueKeysToCommit();
        final int totalPrIssueCount = repositoryPullRequestDao.getNumberOfDistinctIssueKeysToPullRequests();
        if (!status.startExclusively(totalIssueCount, totalPrIssueCount))
        {
            return false;
        }
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
            return false;
        }

        log.info("overall processing took this long {}", stopWatch);

        return true;
    }

    @VisibleForTesting
    static abstract class PrimeCacheClosure implements IssueToMappingClosure
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
