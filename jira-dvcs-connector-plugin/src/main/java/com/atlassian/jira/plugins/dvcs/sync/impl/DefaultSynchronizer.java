package com.atlassian.jira.plugins.dvcs.sync.impl;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.ProgressWriter;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.google.common.base.Function;
import com.google.common.collect.MapMaker;

/**
 * Synchronization services
 */
public class DefaultSynchronizer implements Synchronizer
{
    private final Logger log = LoggerFactory.getLogger(DefaultSynchronizer.class);

	private ExecutorService executorService;

    private OrganizationService organizationService;
    private RepositoryService repositoryService;
    private ChangesetService changesetService;

    public DefaultSynchronizer()
    {
    }

    public void setExecutorService(ExecutorService executorService)
    {
        this.executorService = executorService;
    }

    public void setOrganizationService(OrganizationService organizationService)
    {
        this.organizationService = organizationService;
    }

    public void setRepositoryService(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    public void setChangesetService(ChangesetService changesetService)
    {
        this.changesetService = changesetService;
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

								SynchronisationOperation synchronisationOperation =
                                        new DefaultSynchronisationOperation(key, organizationService, repositoryService, changesetService,
                                                progress);

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
