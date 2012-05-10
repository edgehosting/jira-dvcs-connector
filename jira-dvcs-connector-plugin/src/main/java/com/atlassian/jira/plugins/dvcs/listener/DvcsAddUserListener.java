package com.atlassian.jira.plugins.dvcs.listener;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import com.atlassian.crowd.model.event.UserEvent;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.event.web.action.admin.UserAddedEvent;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;

public class DvcsAddUserListener implements InitializingBean
{

	private static final Logger log = LoggerFactory.getLogger(DvcsAddUserListener.class);

	public static String ORGANIZATION_SELECTOR_REQUEST_PARAM = "dvcs_org_selector";
	public static String USERNAME_PARAM = "username";
	public static String EMAIL_PARAM = "email";

	private final EventPublisher eventPublisher;

	private final OrganizationService organizationService;

	public DvcsAddUserListener(EventPublisher eventPublisher, OrganizationService organizationService)
	{
		super();
		this.eventPublisher = eventPublisher;
		this.organizationService = organizationService;
	}

	@EventListener
	public void onUserAddViaInterface(UserAddedEvent event)
	{

		Map<String, String[]> parameters = event.getRequestParameters();
		String[] organizationIds = parameters.get(ORGANIZATION_SELECTOR_REQUEST_PARAM);

		// continue ? ------------------------------------------------
		if (organizationIds == null || organizationIds.length == 0)
		{
			return;
		}
		// ------------------------------------------------------------

		List<Integer> organizationIdsAsInts = new ArrayList<Integer>(organizationIds.length);
		for (String id : organizationIds)
		{
			organizationIdsAsInts.add(Integer.valueOf(id));
		}

		String username = parameters.get(USERNAME_PARAM)[0];
		String email = parameters.get(EMAIL_PARAM)[0];

		List<Organization> selectedOrganizations = organizationService.getAllByIds(organizationIdsAsInts);

		// invite
		invite(username, email, selectedOrganizations);
	}

	@EventListener
	public void onUserAddViaCrowd(UserEvent event)
	{

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
		invite(username, email, defaultOrganizations);
	}

	private void invite(String username, String email, List<Organization> selectedOrganizations)
	{
		if (CollectionUtils.isNotEmpty(selectedOrganizations)) {
			for (Organization organization : selectedOrganizations)
			{
				// ...
			}
		}
	}

	@Override
	public void afterPropertiesSet() throws Exception
	{
		eventPublisher.register(this);
	}

}