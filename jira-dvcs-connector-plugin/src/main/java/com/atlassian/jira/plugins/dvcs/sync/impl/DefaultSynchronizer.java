package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.smartcommits.CommitMessageParser;
import com.atlassian.jira.plugins.dvcs.smartcommits.SmartcommitOperation;
import com.atlassian.jira.plugins.dvcs.smartcommits.SmartcommitsChangesetsProcessor;
import com.atlassian.jira.plugins.dvcs.smartcommits.SmartcommitsService;
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
	private final SmartcommitsChangesetsProcessor smartcommitsChangesetsProcessor;
	private final ChangesetService changesetService;
	private final CommitMessageParser commitParser;

	private final SmartcommitsService smartcommitService;

	public DefaultSynchronizer(ExecutorService executorService,
			SmartcommitsChangesetsProcessor smartcommitsChangesetsProcessor, ChangesetService changesetService, SmartcommitsService smartcommitService, CommitMessageParser commitParser)
	{
		this.executorService = executorService;
		this.smartcommitsChangesetsProcessor = smartcommitsChangesetsProcessor;
		this.changesetService = changesetService;
		this.smartcommitService = smartcommitService;
		this.commitParser = commitParser;
	}

    // map of ALL Synchronisation Progresses - running and finished ones
    private final ConcurrentMap<Repository, Progress> progressMap = new MapMaker().makeMap();


    @Override
    public void synchronize(Repository repository, SynchronisationOperation operation)
    {
        Progress progress = progressMap.get(repository);
        if (progress==null || progress.isFinished() || progress.isShouldStop())
        {
        	addSynchronisationOperation(repository, operation);
        }
    }

    @Override
    public void stopSynchronization(Repository repository)
    {
    	Progress progress = progressMap.get(repository);
    	if (progress!=null)
    	{
    		progress.setShouldStop(true);
    	}
    }

    private void addSynchronisationOperation(final Repository repository, final SynchronisationOperation operation)
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
                
                    if (progress.isShouldStop())
                    {
                    	return;
                    }
                    
                    operation.synchronise();
                    
                    // on the end of execution
                    if (operation.isSoftSync()) {
                    	smartcommitsChangesetsProcessor.queue(new SmartcommitOperation(changesetService, commitParser, smartcommitService));
                    }
                    //
        
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
