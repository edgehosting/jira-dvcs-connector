package com.atlassian.jira.plugins.dvcs.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;

import com.atlassian.crowd.model.event.Operation;
import com.atlassian.crowd.model.event.UserEvent;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

/**
 * The Class UserAddedExternallyEventProcessor.
 *
 * {@link Runnable} processor that handles logic beside
 * invitations for user added to JIRA i.e. via crowd so not via
 * user interface.
 * 
 * <br /><br />
 * Created on 21.6.2012, 15:22:43
 * <br /><br />
 * @author jhocman@atlassian.com
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

		String email = event.getUser().getEmailAddress();

		List<Organization> defaultOrganizations = organizationService.getAutoInvitionOrganizations();

		// continue ? ------------------------------------------
		if (CollectionUtils.isEmpty(defaultOrganizations))
		{
			return;
		}
		// ------------------------------------------------------

		for (Organization organization : defaultOrganizations)
		{
		    Set<Group> groupSlugs = organization.getDefaultGroupsSlugs();
			Set<String> slugsStrings = extractSlugs(groupSlugs);
			
			if (CollectionUtils.isNotEmpty(slugsStrings))
			{
				DvcsCommunicator communicator = communicatorProvider.getCommunicator(organization.getDvcsType());
				communicator.inviteUser(organization, slugsStrings, email);
			}
		}

	}

	/**
	 * Extract slugs.
	 *
	 * @param groupSlugs the group slugs
	 * @return the collection< string>
	 */
	private Set<String> extractSlugs(Set<Group> groupSlugs)
	{
	    Set<String> slugs = new HashSet<String>();
		
		if (groupSlugs == null) {
			return slugs;
		}
		
		for (Group group : groupSlugs)
		{
			slugs.add(group.getSlug());
		}
		return slugs;
	}
}
