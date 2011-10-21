package com.atlassian.jira.plugins.bitbucket;

import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.SynchronisationOperation;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

/**
 * Synchronization services
 */
public class DefaultSynchronizer implements Synchronizer
{

	private final ExecutorService executorService;
	private final RepositoryManager globalRepositoryManager;

    public DefaultSynchronizer(ExecutorService executorService, @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager)
    {
		this.executorService = executorService;
		this.globalRepositoryManager = globalRepositoryManager;
    }

    // map of ALL Synchronisation Progresses - running and finished ones
    private final ConcurrentMap<SynchronizationKey, DefaultProgress> progressMap = new MapMaker().makeMap();

    // map of currently running Synchronisations
	private final ConcurrentMap<SynchronizationKey, DefaultProgress> operations = new MapMaker()
			.makeComputingMap(new Function<SynchronizationKey, DefaultProgress>()
			{
				public DefaultProgress apply(final SynchronizationKey from)
				{
					final DefaultProgress progress = new DefaultProgress();
					progressMap.put(from, progress);
					
					Runnable runnable = new Runnable()
					{
						public void run()
						{
							try
							{
								progress.start();
								SynchronisationOperation synchronisationOperation = globalRepositoryManager
										.getSynchronisationOperation(from, progress);
								synchronisationOperation.synchronise();
							} catch (SourceControlException sce)
							{
								progress.setError(sce.getMessage());
							} finally
							{
								progress.finish();
								// Removing sync operation from map of running progresses
								operations.remove(from);
							}
						}
					};
					executorService.submit(runnable);
					progress.queued();
					return progress;
				}
			});

    public void synchronize(SourceControlRepository repository)
    {
        operations.get(new SynchronizationKey(repository));
    }

    public void synchronize(SourceControlRepository repository, List<Changeset> changesets)
    {
        operations.get(new SynchronizationKey(repository, changesets));
    }

    public Progress getProgress(final SourceControlRepository repository)
    {
		return progressMap.get(new SynchronizationKey(repository));
    }
}
