package com.atlassian.jira.plugins.dvcs.listener;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.user.UserEvent;
import com.atlassian.jira.event.web.action.admin.UserAddedEvent;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

// 1/ TODO<jho> are these listeners async ?  if no, ... threads for long running tasks
// 2/ what if this listener fails ?
// 3/ user event type on user add via crowd
public class DvcsAddUserListener implements InitializingBean

{
	private static final String SPLITTER = ":";

	private static final Logger log = LoggerFactory.getLogger(DvcsAddUserListener.class);

	public static String ORGANIZATION_SELECTOR_REQUEST_PARAM = "dvcs_org_selector";
	public static String USERNAME_PARAM = "username";
	public static String EMAIL_PARAM = "email";

	private final EventPublisher eventPublisher;

	private final OrganizationService organizationService;

	private final DvcsCommunicatorProvider communicatorProvider;

	public DvcsAddUserListener(EventPublisher eventPublisher, OrganizationService organizationService,
			DvcsCommunicatorProvider communicatorProvider)
	{
		super();
		this.eventPublisher = eventPublisher;
		this.organizationService = organizationService;
		this.communicatorProvider = communicatorProvider;
	}

	@EventListener
	public void onUserAddViaInterface(UserAddedEvent event)
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
		String username = parameters.get(USERNAME_PARAM)[0];
		String email = parameters.get(EMAIL_PARAM)[0];

		// invite
		invite(username, email, invitationsFor);
	}
	
	@EventListener
	public void onUserAddViaCrowd(UserEvent event)
	{
		//com.atlassian.jira.event.user.UserEventType.USER_CREATED;
		
		String username = event.getUser().getName();
		String email = event.getUser().getEmailAddress();

		List<Organization> defaultOrganizations = organizationService.getAutoInvitionOrganizations();

		// continue ? ------------------------------------------
		if (CollectionUtils.isEmpty(defaultOrganizations))
		{
			return;
		}
		// ------------------------------------------------------

		// invite

	}

	private Collection<Invitations> toInvitations(String[] organizationIdsAndGroupSlugs)
	{

		Map<Integer, Invitations> orgIdsToInvitations = new HashMap<Integer, DvcsAddUserListener.Invitations>();

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

	private void invite(String username, String email, Collection<Invitations> invitations)
	{
		if (CollectionUtils.isNotEmpty(invitations))
		{

			for (Invitations invitation : invitations)
			{
				Collection<String> groupSlugs = invitation.groupSlugs;
				Organization organizaton = invitation.organizaton;
				invite(username, email, organizaton, groupSlugs);
			}

		}
	}

	private void invite(String username, String email, Organization organization, Collection<String> groupSlugs)
	{
		if (CollectionUtils.isNotEmpty(groupSlugs))
		{
			DvcsCommunicator communicator = communicatorProvider.getCommunicator(organization.getDvcsType());
			communicator.inviteUser(organization, groupSlugs, email);
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		eventPublisher.register(this);
	}

	static class Invitations
	{
		Organization organizaton;
		Collection<String> groupSlugs = new ArrayList<String>();
	}
}