package com.atlassian.jira.plugins.dvcs.sync.impl;

import com.atlassian.jira.issue.IssueManager;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.plugins.dvcs.sync.Progress;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * Synchronization services
 */
public class DefaultSynchronizer implements Synchronizer
{
    private final Logger log = LoggerFactory.getLogger(DefaultSynchronizer.class);

    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;
	private final ExecutorService executorService;
    private final IssueManager issueManager;

    private RepositoryService repositoryService;

    public DefaultSynchronizer(ExecutorService executorService, IssueManager issueManager, DvcsCommunicatorProvider dvcsCommunicatorProvider)
    {
		this.executorService = executorService;
        this.issueManager = issueManager;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
    }

    public void setRepositoryService(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    // map of ALL Synchronisation Progresses - running and finished ones
    private final ConcurrentMap<SynchronizationKey, DefaultProgress> progressMap = new MapMaker().makeMap();

    // map of currently running Synchronisations
	private final ConcurrentMap<SynchronizationKey, DefaultProgress> operations = new MapMaker()
			.makeComputingMap(new Function<SynchronizationKey, DefaultProgress>()
			{
				@Override
                public DefaultProgress apply(final SynchronizationKey key)
				{
					final DefaultProgress progress = new DefaultProgress();
					progressMap.put(key, progress);
					
					Runnable runnable = new Runnable()
					{
						@Override
                        public void run()
						{
							try
							{
								progress.start();

                                String dvcsType = key.getRepository().getDvcsType();
                                DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(dvcsType);

								SynchronisationOperation synchronisationOperation =
                                        new DefaultSynchronisationOperation(key, repositoryService, communicator, progress, issueManager);

								synchronisationOperation.synchronise();
							} catch (Throwable e)
							{
                                String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
                                progress.setError(errorMessage);
                                log.warn(e.getMessage(), e);
							} finally
							{
								progress.finish();
								// Removing sync operation key map of running progresses
								operations.remove(key);
							}
						}
					};
					executorService.submit(runnable);
					progress.queued();
					return progress;
				}
			});

    @Override
    public void synchronize(Repository repository, boolean softSync)
    {
        operations.get(new SynchronizationKey(repository, softSync));
    }

    @Override
    public Progress getProgress(final Repository repository)
    {
		return progressMap.get(new SynchronizationKey(repository));
    }
}
