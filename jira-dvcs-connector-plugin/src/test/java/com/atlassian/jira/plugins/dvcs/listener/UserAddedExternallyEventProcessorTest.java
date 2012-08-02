package com.atlassian.jira.plugins.dvcs.listener;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import org.hsqldb.lib.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.core.util.collection.EasyList;
import com.atlassian.crowd.model.event.Operation;
import com.atlassian.crowd.model.event.UserEvent;
import com.atlassian.crowd.model.user.UserTemplate;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class UserAddedExternallyEventProcessorTest
{
	
	@Mock
	OrganizationService organizationServiceMock;

	@Mock
	DvcsCommunicatorProvider communicatorProviderMock;
	
	@Mock
	DvcsCommunicator communicatorMock;

	@Captor
	ArgumentCaptor<String> emailCaptor;
	
	@Captor
	ArgumentCaptor<Collection<String>> slugsCaptor;

	// tested object
	private UserAddedExternallyEventProcessor processor;

	public UserAddedExternallyEventProcessorTest()
	{
		
	}
	
	@Before
	public void setUp () {
	
		processor = new UserAddedExternallyEventProcessor(sampleEvent(), organizationServiceMock, communicatorProviderMock);
		
		when(communicatorProviderMock.getCommunicator(anyString())).thenReturn(communicatorMock);
	}
	
	@Test
	public void testRunShouldInvite() {
		
		when(organizationServiceMock.getAutoInvitionOrganizations()).thenReturn(sampleOrganizations());
		
		processor.run();
		
		verify(communicatorMock).inviteUser(isA(Organization.class), slugsCaptor.capture(), emailCaptor.capture());
		
		Assert.assertTrue(slugsCaptor.getAllValues().size() == 1 &&
				slugsCaptor.getAllValues().get(0).contains("A") &&
				slugsCaptor.getAllValues().get(0).contains("B")
				);
		

		Assert.assertTrue(emailCaptor.getAllValues().size() == 1 &&
				emailCaptor.getAllValues().get(0).equals("principal@example.com") 
				);
		
	}

	@Test
	public void testRunNotCreatedEventTypeShouldntInvite() {
		
		processor = new UserAddedExternallyEventProcessor(new UserEvent(Operation.UPDATED, null, null, null, null), organizationServiceMock, communicatorProviderMock);
		 
		processor.run();
		
		verifyNoMoreInteractions(organizationServiceMock);
		verifyNoMoreInteractions(communicatorMock);
	}
	
	@Test
	public void testRunNoDefaultGroupsShouldntInvite() {
		
		when(organizationServiceMock.getAutoInvitionOrganizations()).thenReturn(Collections.EMPTY_LIST);
		
		processor.run();
		
		verifyNoMoreInteractions(communicatorMock);
	}

	
	private List<Organization> sampleOrganizations()
	{
		Organization org = new Organization();
        org.setDefaultGroupsSlugs(Sets.newHashSet(new Group("A"), new Group("B")));
		return EasyList.build(org);
	}

	private UserEvent sampleEvent()
	{
		UserTemplate user = new UserTemplate("principal");
		user.setEmailAddress("principal@example.com");
		UserEvent userEvent = new UserEvent(Operation.CREATED, null, user, null, null);
		return userEvent;
	}
}

