package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.ProjectCreatedEvent;

/**
 * Simple JIRA listener using the atlassian-event library and demonstrating
 * plugin lifecycle integration.
 */
public class ProjectCreatedListener implements InitializingBean, DisposableBean
{
	private final EventPublisher eventPublisher;
	
	/**
	 * Constructor.
	 * @param eventPublisher
	 *            injected {@code EventPublisher} implementation.
	 * @param bitbucketLinker
	 */
	public ProjectCreatedListener(EventPublisher eventPublisher)
	{
		this.eventPublisher = eventPublisher;
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
		
	}

}
