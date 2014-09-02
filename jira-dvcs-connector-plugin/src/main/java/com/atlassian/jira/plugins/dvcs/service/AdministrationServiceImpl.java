package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.RepositoryTransformer;
import com.atlassian.jira.plugins.dvcs.event.DevSummaryChangedEvent;
import com.atlassian.jira.plugins.dvcs.event.EventService;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
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
    private ThreadEvents threadEvents;

    @Resource
    private ChangesetDao changesetDao;

    @Resource (name = "eventServiceImpl")
    EventService eventService;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private RepositoryTransformer repositoryTransformer;

    @Override
    public void forEachIssueToCommitMapping()
    {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        final Set<Integer> repositoryIds = new HashSet<Integer>();

//        ThreadEventsCaptor threadEventCaptor = threadEvents.startCapturing();

        changesetDao.forEachIssueToCommitMapping(new ChangesetDao.ForEachIssueToCommitMappingClosure()
        {
            @Override
            public void execute(final String dvcsType, final int repositoryId, final Set<String> issueKeys)
            {
                eventService.storeEvent(repositoryId, new DevSummaryChangedEvent(repositoryId, dvcsType, issueKeys), false);
                repositoryIds.add(repositoryId);
//                repositoryById.put(repositoryId, repositoryTransformer.transform())
//                threadEvents.broadcast(new DevSummaryChangedEvent(repositoryId, dvcsType, issueKey));
            }
        });

//        threadEventCaptor.stopCapturing();
//
//        threadEventCaptor.processEach(SyncEvent.class, new ThreadEventsCaptor.Closure<SyncEvent>()
//        {
//            @Override
//            public void process(@Nonnull SyncEvent event)
//            {
//                eventService.storeEvent(1, event, false);
//            }
//        });

        eventService.dispatchEvents(repositoryIds);

        log.info("overall processing took this long {}", stopWatch);
    }
}
