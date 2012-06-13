package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.ProjectCreatedEvent;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;

/**
 * Simple JIRA listener using the atlassian-event library and demonstrating
 * plugin lifecycle integration.
 */
public class ProjectCreatedListener implements InitializingBean, DisposableBean
{
	private static final Logger log = LoggerFactory.getLogger(ProjectCreatedListener.class);

	private final EventPublisher eventPublisher;
	private final BitbucketLinker bitbucketLinker;
	private final RepositoryService repositoryService;
	
	/**
	 * Constructor.
	 * @param eventPublisher
	 *            injected {@code EventPublisher} implementation.
	 * @param bitbucketLinker
	 */
	public ProjectCreatedListener(EventPublisher eventPublisher,
	        @Qualifier("defferedBitbucketLinker") BitbucketLinker bitbucketLinker,
	        RepositoryService repositoryService)
	{
		this.eventPublisher = eventPublisher;
		this.bitbucketLinker = bitbucketLinker;
		this.repositoryService = repositoryService;
	}

	/**
	 * Called when the plugin has been enabled.
	 * 
	 * @throws Exception
	 */
	@Override
	public void afterPropertiesSet() throws Exception
	{
		// register ourselves with the EventPublisher
		eventPublisher.register(this);
	}

	/**
	 * Called when the plugin is being disabled or removed.
	 * 
	 * @throws Exception
	 */
	@Override
	public void destroy() throws Exception
	{
		// unregister ourselves with the EventPublisher
		eventPublisher.unregister(this);
	}

	/**
	 * Receives any {@code ProjectCreatedEvent}s sent by JIRA.
	 * 
	 * @param projectCreatedEvent
	 *            the ProjectCreatedEvent passed to us
	 */
	@EventListener
	public void onProjectCreated(ProjectCreatedEvent projectCreatedEvent)
	{
		log.debug("New project [" + projectCreatedEvent.getId()
		        + "] created, updating repository links");
		List<Repository> allRepositories = repositoryService.getAllRepositories();
		for (Repository repository : allRepositories)
        {
			if (repository.isLinked())
			{
				bitbucketLinker.linkRepository(repository);
			}
        }
	}

}
