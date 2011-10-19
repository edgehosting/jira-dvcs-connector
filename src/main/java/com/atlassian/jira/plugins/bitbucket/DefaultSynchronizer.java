package com.atlassian.jira.plugins.bitbucket;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;

import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.SynchronisationOperation;
import com.atlassian.templaterenderer.TemplateRenderer;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.MapMaker;

/**
 * Synchronization services
 */
public class DefaultSynchronizer implements Synchronizer
{
    private class Coordinator
    {
    	// could this get any more complicated? 
    	// TODO add error tracking for synchronisation
		private final ConcurrentMap<SynchronizationKey, DefaultProgress> operations = new MapMaker().makeComputingMap(new Function<SynchronizationKey, DefaultProgress>()
		{
			public DefaultProgress apply(final SynchronizationKey from)
			{
				Callable<Object> callable = new Callable<Object>()
				{
					public Object call() throws Exception
					{
						try
						{
							SynchronisationOperation synchronisationOperation = globalRepositoryManager
									.getSynchronisationOperation(from, progressProvider);
							synchronisationOperation.synchronise();
						} finally
						{
							operations.remove(from);
						}
						return null;
					}
				};
				return new DefaultProgress(templateRenderer, from, executorService.submit(callable));
			}
		});

        
		private final Function<SynchronizationKey, Progress> progressProvider = new Function<SynchronizationKey, Progress>()
		{
			public DefaultProgress apply(SynchronizationKey from)
			{
				return coordinator.operations.get(from);
			}
		};
        
        private final ExecutorService executorService;
        private final TemplateRenderer templateRenderer;

        public Coordinator(ExecutorService executorService, TemplateRenderer templateRenderer)
        {
            this.executorService = executorService;
            this.templateRenderer = templateRenderer;
        }
    }

    private final Coordinator coordinator;
	private final RepositoryManager globalRepositoryManager;

    public DefaultSynchronizer(ExecutorService executorService, TemplateRenderer templateRenderer, 
                               @Qualifier("globalRepositoryManager") RepositoryManager globalRepositoryManager)
    {
		this.globalRepositoryManager = globalRepositoryManager;
        this.coordinator = new Coordinator(executorService, templateRenderer);
    }

    public void synchronize(SourceControlRepository repository)
    {
        coordinator.operations.get(new SynchronizationKey(repository));
    }

    public void synchronize(SourceControlRepository repository, List<Changeset> changesets)
    {
        coordinator.operations.get(new SynchronizationKey(repository, changesets));
    }

    public Iterable<DefaultProgress> getProgress()
    {
        return coordinator.operations.values();
    }

    public Iterable<DefaultProgress> getProgress(final SourceControlRepository repository)
    {
        return Iterables.filter(coordinator.operations.values(), new Predicate<DefaultProgress>()
        {
            public boolean apply(DefaultProgress input)
            {
                return input.matches(repository);
            }
        });
    }
}
