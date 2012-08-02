package com.atlassian.jira.plugins.dvcs.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;

import com.atlassian.jira.event.web.action.admin.UserAddedEvent;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

/**
 * 
 * {@link Runnable} processor that handles logic beside
 * invitations for user added to JIRA via
 * user interface.
 *
 * 
 * <br /><br />
 * Created on 21.6.2012, 15:32:33
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
class UserAddedViaInterfaceEventProcessor implements Runnable
{

	/** The ORGANIZATIO n_ selecto r_ reques t_ param. */
	public static String ORGANIZATION_SELECTOR_REQUEST_PARAM = "dvcs_org_selector";
		
	/** The EMAI l_ param. */
	public static String EMAIL_PARAM = "email";

	/** The Constant SPLITTER. */
	private static final String SPLITTER = ":";

	/** The event. */
	private final UserAddedEvent event;
	
	/** The organization service. */
	private final OrganizationService organizationService;
	
	/** The communicator provider. */
	private final DvcsCommunicatorProvider communicatorProvider;

	/**
	 * Instantiates a new user added via interface event processor.
	 *
	 * @param event the event
	 * @param organizationService the organization service
	 * @param communicatorProvider the communicator provider
	 */
	public UserAddedViaInterfaceEventProcessor(UserAddedEvent event, OrganizationService organizationService,
			DvcsCommunicatorProvider communicatorProvider)
	{
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

		Map<String, String[]> parameters = event.getRequestParameters();
		String[] organizationIdsAndGroupSlugs = parameters.get(ORGANIZATION_SELECTOR_REQUEST_PARAM);

		// continue ? ------------------------------------------------
		if (organizationIdsAndGroupSlugs == null || organizationIdsAndGroupSlugs.length == 0)
		{
			return;
		}
		// ------------------------------------------------------------

		Collection<Invitations> invitationsFor = toInvitations(organizationIdsAndGroupSlugs);
		String email = parameters.get(EMAIL_PARAM)[0];

		// invite
		invite(email, invitationsFor);

	}

	/**
	 * To invitations.
	 *
	 * @param organizationIdsAndGroupSlugs the organization ids and group slugs
	 * @return the collection
	 */
	private Collection<Invitations> toInvitations(String[] organizationIdsAndGroupSlugs)
	{

		Map<Integer, Invitations> orgIdsToInvitations = new HashMap<Integer, Invitations>();

		for (String requestParamToken : organizationIdsAndGroupSlugs)
		{

			String[] tokens = requestParamToken.split(SPLITTER);
			Integer orgId = Integer.parseInt(tokens[0]);
			String slug = tokens[1];
			Invitations existingInvitations = orgIdsToInvitations.get(orgId);

			//
			// first time organization ?
			if (existingInvitations == null)
			{
				Invitations newInvitations = new Invitations();
				newInvitations.organizaton = organizationService.get(orgId, false);
				orgIdsToInvitations.put(orgId, newInvitations);

				existingInvitations = newInvitations;
			}

			//
			existingInvitations.groupSlugs.add(slug);
		}

		return orgIdsToInvitations.values();
	}

	/**
	 * Invite.
	 *
	 * @param email the email
	 * @param invitations the invitations
	 */
	private void invite( String email, Collection<Invitations> invitations)
	{
		if (CollectionUtils.isNotEmpty(invitations))
		{

			for (Invitations invitation : invitations)
			{
				Collection<String> groupSlugs = invitation.groupSlugs;
				Organization organizaton = invitation.organizaton;
				invite(email, organizaton, groupSlugs);
			}

		}
	}

	/**
	 * Invite.
	 *
	 * @param email the email
	 * @param organization the organization
	 * @param groupSlugs the group slugs
	 */
	private void invite(String email, Organization organization, Collection<String> groupSlugs)
	{
		if (CollectionUtils.isNotEmpty(groupSlugs))
		{
			DvcsCommunicator communicator = communicatorProvider.getCommunicator(organization.getDvcsType());
			communicator.inviteUser(organization, groupSlugs, email);
		}
	}

	/**
	 * The Class Invitations.
	 */
	static class Invitations
	{
		
		/** The organizaton. */
		Organization organizaton;
		
		/** The group slugs. */
		Collection<String> groupSlugs = new ArrayList<String>();
	}
}
