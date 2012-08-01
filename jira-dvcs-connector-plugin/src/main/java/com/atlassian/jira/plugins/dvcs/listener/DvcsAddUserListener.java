package com.atlassian.jira.plugins.dvcs.listener;

import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.crowd.model.event.Operation;
import com.atlassian.crowd.model.event.UserEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.web.action.admin.UserAddedEvent;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.plugin.event.PluginEventListener;
import com.atlassian.plugin.event.events.PluginDisabledEvent;
import com.atlassian.plugin.event.events.PluginUninstalledEvent;

/**
 * 
 * Listens to user events (just for <code>CREATED</code> type).
 *
 * Handler methods run asynchronously and are safe to fail.
 * That means that it does not corrupt process of adding the user
 * because of some unexpected error at this place. 
 * 
 * @see #onUserAddViaInterface(UserAddedEvent)
 * @see #onUserAddViaCrowd(UserEvent)
 *
 * <br /><br />
 * Created on 21.6.2012, 14:07:34
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class DvcsAddUserListener
{
	
	/** The Constant log. */
	private static final Logger log = LoggerFactory.getLogger(DvcsAddUserListener.class);

	/** The event publisher. */
	private final EventPublisher eventPublisher;

	/** The organization service. */
	private final OrganizationService organizationService;

	/** The communicator provider. */
	private final DvcsCommunicatorProvider communicatorProvider;

    private final ExecutorService executorService;

	/**
	 * The Constructor.
	 *
	 * @param eventPublisher the event publisher
	 * @param organizationService the organization service
	 * @param communicatorProvider the communicator provider
	 */
	public DvcsAddUserListener(EventPublisher eventPublisher, OrganizationService organizationService,
			DvcsCommunicatorProvider communicatorProvider, ExecutorService executorService)
	{
		this.eventPublisher = eventPublisher;
		this.organizationService = organizationService;
		this.communicatorProvider = communicatorProvider;
        this.executorService = executorService;
	}

	/**
	 * Handler method for add user via interface.
	 *
	 * @param event the event object
	 */
	@EventListener
	public void onUserAddViaInterface(final UserAddedEvent event)
	{
        if (event == null)
        {
            return;
        }
		String onFailMessage = "Failed to handle add user via interface event [ " + event + ", params =  " + event.getRequestParameters() + "] ";
		Runnable task = new UserAddedViaInterfaceEventProcessor(event, organizationService, communicatorProvider);
        safeExecute(task, onFailMessage);
	}

	/**
	 * Handler method for add user externally; i.e
	 *
	 * @param event the event object
	 */
	@EventListener
	public void onUserAddViaCrowd(UserEvent event)
	{
	    if ((event==null) || Operation.CREATED != event.getOperation())
	    {
	        return;
	    }
        Runnable task = new UserAddedExternallyEventProcessor(event, organizationService, communicatorProvider);
        String onFailMessage = "Failed to handle add user externally event [ " + event + ", user =  " + event.getUser() + "] ";
        
        safeExecute(task, onFailMessage);
	}

	/**
	 * Wraps executorService.submit(task) method
	 * invocation with <code>try-catch</code> block
	 * to ensure that no exception is propagated up.
	 *
	 * @param task the task
	 * @param onFailMessage the on fail message
	 */
    private void safeExecute(Runnable task, String onFailMessage)
    {
        try
        {
            if (task != null)
            {
                executorService.submit(task);
            }
        } catch (Throwable t)
        {
            log.warn(onFailMessage, t);
        }
    }

	@PluginEventListener
    public void onPluginDisabled(PluginDisabledEvent event)
    {
       unregisterSelf();
    }

	@PluginEventListener
	public void onPluginUninstalled(PluginUninstalledEvent event)
	{
		unregisterSelf();
	}

	private void unregisterSelf()
	{
		try
		{
			eventPublisher.unregister(this);
		} catch (Exception e)
		{
			log.warn("Failed to unregister " + this + ", cause message is " + e.getMessage());
		}
	}
}