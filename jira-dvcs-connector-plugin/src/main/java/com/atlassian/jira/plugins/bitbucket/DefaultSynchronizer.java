package com.atlassian.jira.plugins.bitbucket;

import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.api.Synchronizer;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

/**
 * Synchronization services
 */
public class DefaultSynchronizer implements Synchronizer
{
    private final Logger log = LoggerFactory.getLogger(DefaultSynchronizer.class);

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
				@Override
                public DefaultProgress apply(final SynchronizationKey from)
				{
					final DefaultProgress progress = new DefaultProgress();
					progressMap.put(from, progress);
					
					Runnable runnable = new Runnable()
					{
						@Override
                        public void run()
						{
							try
							{
								progress.start();
								SynchronisationOperation synchronisationOperation = globalRepositoryManager
										.getSynchronisationOperation(from, progress);
								synchronisationOperation.synchronise();
							} catch (Throwable e)
							{
                                String errorMessage = e.getMessage() == null ? e.toString() : e.getMessage();
                                progress.setError(errorMessage);
                                log.warn(e.getMessage(), e);
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

    @Override
    public void synchronize(SourceControlRepository repository)
    {
        operations.get(new SynchronizationKey(repository));
    }

    @Override
    public void synchronize(SourceControlRepository repository, boolean softSync)
    {
        operations.get(new SynchronizationKey(repository, softSync));
    }

    @Override
    public Progress getProgress(final SourceControlRepository repository)
    {
		return progressMap.get(new SynchronizationKey(repository));
    }
}
