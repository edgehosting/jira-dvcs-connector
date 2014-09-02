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
    EventService eventService;

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

        PrimeCacheClosure closure = new PrimeCacheClosure();
        changesetDao.forEachIssueToCommitMapping(closure);

        eventService.dispatchEvents(closure.repositoryIds);

        status.finished();

        log.info("overall processing took this long {}", stopWatch);

        return true;
    }

    @VisibleForTesting
    class PrimeCacheClosure implements ChangesetDao.ForEachIssueToCommitMappingClosure
    {

        private final Set<Integer> repositoryIds = new HashSet<Integer>();

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
