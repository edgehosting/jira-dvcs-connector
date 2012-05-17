package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.google.common.collect.MapMaker;

/**
 * Synchronization services
 */
public class DefaultSynchronizer implements Synchronizer
{
    private final Logger log = LoggerFactory.getLogger(DefaultSynchronizer.class);

	private final ExecutorService executorService;

    public DefaultSynchronizer(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    // map of ALL Synchronisation Progresses - running and finished ones
    private final ConcurrentMap<Repository, Progress> progressMap = new MapMaker().makeMap();


    @Override
    public void synchronize(Repository repository, SynchronisationOperation operation)
    {
        if (!isSyncRunning(repository))
        {
            addSynchronisatinoOperation(repository, operation);
        }
    }

    private boolean isSyncRunning(Repository repository)
    {
        Progress progress = progressMap.get(repository);
        if (progress==null)
        {
            return false;
        }
        return !progress.isFinished();
    }

    private void addSynchronisatinoOperation(final Repository repository, final SynchronisationOperation operation)
    {
        final DefaultProgress progress = operation.getProgress();
        progressMap.put(repository, progress);
        
        Runnable runnable = new Runnable()
        {
            @Override
            public void run()
            {
                try
                {
                    progress.start();
                    operation.synchronise();
                } catch (Throwable e)
                {
                    String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
                    progress.setError(errorMessage);
                    log.warn(e.getMessage(), e);
                } finally
                {
                    progress.finish();
                }
            }
        };
        executorService.submit(runnable);
        progress.queued();
    }

    @Override
    public Progress getProgress(final Repository repository)
    {
		return progressMap.get(repository);
    }
}
