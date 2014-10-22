package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.plugin.spring.scanner.annotation.export.ExportAsService;
import com.atlassian.util.concurrent.Promise;
import com.atlassian.util.concurrent.Promises;
import com.atlassian.util.concurrent.ThreadFactories;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nonnull;

@ExportAsService (SmartcommitsChangesetsProcessor.class)
@Component
public class DefaultSmartcommitsChangesetsProcessor implements SmartcommitsChangesetsProcessor, DisposableBean
{

    /**
     * Logger of this class.
     */
    private static final Logger log = LoggerFactory.getLogger(DefaultSmartcommitsChangesetsProcessor.class);

    private final ListeningExecutorService executor;
    private final SmartcommitsService smartcommitService;
    private final CommitMessageParser commitParser;
    private final ChangesetDao changesetDao;

    @Autowired
    public DefaultSmartcommitsChangesetsProcessor(ChangesetDao changesetDao, SmartcommitsService smartcommitService,
            CommitMessageParser commitParser)
    {
        this.changesetDao = changesetDao;
        this.smartcommitService = smartcommitService;
        this.commitParser = commitParser;

        // a listening decorator returns ListenableFuture, which we then wrap in a Promise. using JDK futures directly
        // leads to an extra thread being created for the lifetime of the Promise (see Guava JdkFutureAdapters)
        executor = MoreExecutors.listeningDecorator(createThreadPool());
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
    @Nonnull
    @Override
    public Promise<Void> startProcess(Progress forProgress, Repository repository, ChangesetService changesetService)
    {
        return Promises.forListenableFuture(executor.submit(
                new SmartcommitOperation(changesetDao, commitParser, smartcommitService, forProgress, repository, changesetService)
        ));
    }

    private ThreadPoolExecutor createThreadPool()
    {
        return new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(),
                ThreadFactories.namedThreadFactory("DVCSConnector.SmartCommitsProcessor"));
    }
}
