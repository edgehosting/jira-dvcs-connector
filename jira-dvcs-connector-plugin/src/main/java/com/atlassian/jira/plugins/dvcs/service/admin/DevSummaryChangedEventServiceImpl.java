package com.atlassian.jira.plugins.dvcs.service.admin;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;
import com.atlassian.jira.plugins.dvcs.dao.IssueToMappingFunction;
import com.atlassian.jira.plugins.dvcs.event.DevSummaryChangedEvent;
import com.atlassian.jira.plugins.dvcs.event.EventService;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.util.concurrent.ThreadFactories;
import com.atlassian.plugin.spring.scanner.annotation.imports.ComponentImport;
import com.atlassian.sal.api.executor.ThreadLocalDelegateExecutorFactory;
import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang3.time.StopWatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import javax.annotation.Resource;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Service that manages generation of dev summary changed events, typically used for priming the dev summary cache.
 */
@Component
public class DevSummaryChangedEventServiceImpl
{
    private static final ThreadFactory THREAD_FACTORY =
            ThreadFactories.namedThreadFactory("DVCSConnector.DevSummaryChangedEventServiceImpl");
    private static final Logger log = LoggerFactory.getLogger(DevSummaryChangedEventServiceImpl.class);

    @Resource
    private ChangesetDao changesetDao;

    @Resource
    private RepositoryPullRequestDao repositoryPullRequestDao;

    @Resource
    private OrganizationService organizationService;

    @Resource (name = "eventServiceImpl")
    private EventService eventService;

    @Resource
    private DevSummaryCachePrimingStatus status;

    final ThreadLocalDelegateExecutorFactory executorFactory;

    private final Executor executor;

    @Autowired
    public DevSummaryChangedEventServiceImpl(@ComponentImport final ThreadLocalDelegateExecutorFactory executorFactory)
    {
        this.executorFactory = checkNotNull(executorFactory);
        executor = executorFactory.createExecutor(Executors.newSingleThreadExecutor(THREAD_FACTORY));
    }

    /**
     * Starts priming the cache, runs in a separate thread.
     *
     * @param pageSize number of issue mappings to fetch per run
     * @return true if the job could be started, false if there is a job in progress
     */
    public boolean generateDevSummaryEvents(final int pageSize)
    {
        final int totalIssueCount = changesetDao.getNumberOfIssueKeysToChangeset();
        final int totalPrIssueCount = repositoryPullRequestDao.getNumberOfIssueKeysToPullRequests();
        if (status.startExclusively(totalIssueCount, totalPrIssueCount))
        {
            executor.execute(new Runnable()
            {
                @Override
                public void run()
                {
                    startPriming(pageSize);
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
    void startPriming(int pageSize)
    {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        try
        {
            List<Organization> organizations = organizationService.getAll(true);
            final ChangesetPrimeCacheClosure changesetPrimeCacheClosure = new ChangesetPrimeCacheClosure(eventService, status);
            final PullRequestPrimeCacheClosure pullRequestPrimeCacheClosure = new PullRequestPrimeCacheClosure(eventService, status);
            for (Organization organization : organizations)
            {
                for (Repository repository : organization.getRepositories())
                {
                    if (repository.isLinked())
                    {
                        changesetDao.forEachIssueKeyMapping(organization, repository, pageSize, changesetPrimeCacheClosure);
                        repositoryPullRequestDao.forEachIssueKeyMapping(organization, repository, pageSize, pullRequestPrimeCacheClosure);
                    }
                }
            }
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
    static class ChangesetPrimeCacheClosure extends PrimeCacheClosure
    {
        ChangesetPrimeCacheClosure(final EventService eventService, final DevSummaryCachePrimingStatus status)
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

    public DevSummaryCachePrimingStatus getEventGenerationStatus()
    {
        return status;
    }

    public void stopGeneration()
    {
        status.stopped();
    }
}
