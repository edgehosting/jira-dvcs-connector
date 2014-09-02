package com.atlassian.jira.plugins.dvcs.service.admin;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.event.DevSummaryChangedEvent;
import com.atlassian.jira.plugins.dvcs.event.EventService;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;
import javax.annotation.Resource;

public class AdministrationServiceImpl implements AdministrationService
{

    private static final Logger log = LoggerFactory.getLogger(AdministrationServiceImpl.class);

    @Resource
    private ChangesetDao changesetDao;

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
        final int totalPullRequestCount = 0;
        if (!status.startExclusively(totalIssueCount, totalPullRequestCount))
        {
            return false;
        }

        PrimeCacheClosure closure = new PrimeCacheClosure(eventService, status);
        try
        {
            changesetDao.forEachIssueToCommitMapping(closure);
            eventService.dispatchEvents(closure.repositoryIds);
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
    static class PrimeCacheClosure implements ChangesetDao.ForEachIssueToCommitMappingClosure
    {
        private final Set<Integer> repositoryIds = new HashSet<Integer>();
        private final EventService eventService;
        private final DevSummaryCachePrimingStatus status;

        PrimeCacheClosure(final EventService eventService, final DevSummaryCachePrimingStatus status)
        {
            this.eventService = eventService;
            this.status = status;
        }

        @Override
        public boolean execute(final String dvcsType, final int repositoryId, final Set<String> issueKeys)
        {
            eventService.storeEvent(repositoryId, new DevSummaryChangedEvent(repositoryId, dvcsType, issueKeys), false);
            repositoryIds.add(repositoryId);
            status.completedIssueKeyBatch(issueKeys.size());
            return !status.isStopped();
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
