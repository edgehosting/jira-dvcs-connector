package com.atlassian.jira.plugins.dvcs.listener;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.Collection;
import java.util.Map;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.atlassian.core.util.map.EasyMap;
import com.atlassian.jira.event.web.action.admin.UserAddedEvent;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

@RunWith(MockitoJUnitRunner.class)
public class UserAddedViaInterfaceEventProcessorTest
{

	UserAddedViaInterfaceEventProcessor processor;
	
	@Mock
	private DvcsCommunicatorProvider communicatorProviderMock;

	@Mock
	private DvcsCommunicator communicator;
	
	@Mock
	private OrganizationService organizationServiceMock;

	@Mock
	Organization organizationMock;

	@Captor
	ArgumentCaptor<String> email;
	
	@Captor
	ArgumentCaptor<Collection<String>> slugs;

	public UserAddedViaInterfaceEventProcessorTest()
	{

	}
	
	@Before
	public void setUp () {
		
		when(communicatorProviderMock.getCommunicator(anyString())).thenReturn(communicator);
		when(organizationMock.getDvcsType()).thenReturn("bitbucket");
		when(organizationServiceMock.get(anyInt(), eq(false))).thenReturn(organizationMock);
	}

	@Test
	public void testRunShouldInviteToMultipleGroups() {
		
		UserAddedEvent event = new UserAddedEvent(sampleRequestParameters());
		processor = new UserAddedViaInterfaceEventProcessor(event, organizationServiceMock, communicatorProviderMock);
		
		processor.run();

		verify(organizationServiceMock, times(2)).get(anyInt(), eq(false));

		verify(communicator, times(2)).inviteUser(isA(Organization.class), slugs.capture(), email.capture());
		
		Assert.assertTrue(slugs.getAllValues().size() == 2 &&
						slugs.getAllValues().get(0).size() == 2 &&
						slugs.getAllValues().get(0).contains("developers") &&
						slugs.getAllValues().get(0).contains("managers") &&
						slugs.getAllValues().get(1).size() == 1 &&
						slugs.getAllValues().get(0).contains("developers") 
					);
		
		Assert.assertTrue(email.getAllValues().size() ==  2 && email.getAllValues().get(0).contains("new@example.com"));
	}
	
	@Test
	public void testRunNoGrpupsHasBeenSelected() {
		
		UserAddedEvent event = new UserAddedEvent(EasyMap.build());
		
		processor = new UserAddedViaInterfaceEventProcessor(event, organizationServiceMock, communicatorProviderMock);
		
		processor.run();
		
		verifyNoMoreInteractions(organizationServiceMock);
		verifyNoMoreInteractions(communicatorProviderMock);
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, String[]> sampleRequestParameters()
	{
		return EasyMap.build(UserAddedViaInterfaceEventProcessor.ORGANIZATION_SELECTOR_REQUEST_PARAM, new String [] {
				
				"2:developers", // org id + groups slug
				"2:managers",
				"3:developers"
				
		},
		
			"username", new String [] { "new_user_principal" },
		
			"email", new String [] {"new@example.com"}
		
		);
		
	}
}
