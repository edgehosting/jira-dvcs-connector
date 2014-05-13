package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.util.concurrent.ThreadFactories;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class DefaultSmartcommitsChangesetsProcessor implements SmartcommitsChangesetsProcessor, DisposableBean
{

    /**
     * Logger of this class.
     */
    private static final Logger log = LoggerFactory.getLogger(DefaultSmartcommitsChangesetsProcessor.class);

    private final ThreadPoolExecutor executor;
    private final SmartcommitsService smartcommitService;
    private final CommitMessageParser commitParser;
    private final ChangesetDao changesetDao;

    public DefaultSmartcommitsChangesetsProcessor(ChangesetDao changesetDao, SmartcommitsService smartcommitService,
                                                  CommitMessageParser commitParser)
    {
        this.changesetDao = changesetDao;
        this.smartcommitService = smartcommitService;
        this.commitParser = commitParser;

        executor = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                ThreadFactories.namedThreadFactory(DefaultSmartcommitsChangesetsProcessor.class.getSimpleName()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() throws Exception
    {
        executor.shutdown();
        if (!executor.awaitTermination(1, TimeUnit.MINUTES))
        {
            log.error("Unable properly shutdown queued tasks.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void startProcess(Progress forProgress, Repository repository, ChangesetService changesetService)
    {
        executor.execute(new SmartcommitOperation(changesetDao, commitParser, smartcommitService, forProgress, repository, changesetService));
    }

}
