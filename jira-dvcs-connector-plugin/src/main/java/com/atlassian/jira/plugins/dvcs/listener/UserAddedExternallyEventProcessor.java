package com.atlassian.jira.plugins.dvcs.listener;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import com.atlassian.crowd.model.event.Operation;
import com.atlassian.crowd.model.event.UserEvent;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

/**
 * {@link Runnable} processor that handles logic beside
 * invitations for user added to JIRA i.e. via crowd so not via
 * user interface.
 * 
 * <br /><br />
 * Created on 21.6.2012, 15:22:43
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
class UserAddedExternallyEventProcessor implements Runnable
{

	/** The event. */
	private final UserEvent event;

	/** The organization service. */
	private final OrganizationService organizationService;

	/** The communicator provider. */
	private final DvcsCommunicatorProvider communicatorProvider;

	/**
	 * The Constructor.
	 * 
	 * @param event
	 *            the event
	 * @param organizationService
	 *            the organization service
	 * @param communicatorProvider
	 *            the communicator provider
	 */
	public UserAddedExternallyEventProcessor(UserEvent event, OrganizationService organizationService,
			DvcsCommunicatorProvider communicatorProvider)
	{
		super();
		this.event = event;
		this.organizationService = organizationService;
		this.communicatorProvider = communicatorProvider;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void run()
	{

		if (Operation.CREATED != event.getOperation())
		{
			return;
		}

		String username = event.getUser().getName();
		String email = event.getUser().getEmailAddress();

		List<Organization> defaultOrganizations = organizationService.getAutoInvitionOrganizations();

		// continue ? ------------------------------------------
		if (CollectionUtils.isEmpty(defaultOrganizations))
		{
			return;
		}
		// ------------------------------------------------------

		// TODO invite

	}
}
