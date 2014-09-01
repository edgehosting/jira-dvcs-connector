package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.event.DevSummaryChangedEvent;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import javax.annotation.Resource;

public class AdministrationServiceImpl implements AdministrationService
{

    private static final Logger log = LoggerFactory.getLogger(AdministrationServiceImpl.class);

    @Resource
    private ThreadEvents threadEvents;

    @Resource
    private ChangesetDao changesetDao;

    @Override
    public void forEachIssueToCommitMapping()
    {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        changesetDao.forEachIssueToCommitMapping(new ChangesetDao.ForEachIssueToCommitMappingClosure()
        {
            @Override
            public void execute(final String dvcsType, final int repositoryId, final Set<String> issueKey)
            {
                threadEvents.broadcast(new DevSummaryChangedEvent(repositoryId, dvcsType, issueKey));
            }
        });
        log.info("overall processing took this long {}", stopWatch);
    }
}
