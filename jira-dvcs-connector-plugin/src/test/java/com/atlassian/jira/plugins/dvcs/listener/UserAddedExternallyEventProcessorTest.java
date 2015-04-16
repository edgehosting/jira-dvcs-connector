package com.atlassian.jira.plugins.dvcs.listener;

import com.atlassian.core.util.collection.EasyList;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.OrganizationService;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import com.google.common.collect.Sets;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@SuppressWarnings ("unchecked")
public class UserAddedExternallyEventProcessorTest
{
    @Mock
    ApplicationUser userMock;

    @Mock
    OrganizationService organizationServiceMock;

    @Mock
    DvcsCommunicatorProvider communicatorProviderMock;

    @Mock
    UserManager userManager;

    @Mock
    GroupManager groupManager;

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

    @BeforeMethod
    public void setUp()
    {
        MockitoAnnotations.initMocks(this);

        when(userMock.getEmailAddress()).thenReturn(sampleEmail());
        when(userManager.getUserByName(eq(sampleUsername()))).thenReturn(userMock);

        processor = new UserAddedExternallyEventProcessor(sampleUsername(), organizationServiceMock, communicatorProviderMock, userManager, groupManager);

        when(communicatorProviderMock.getCommunicator(anyString())).thenReturn(communicatorMock);
    }

    @Test
    public void testRunShouldInvite()
    {
        when(organizationServiceMock.getAll(false)).thenReturn(sampleOrganizations());

        processor.run();

        verify(communicatorMock).inviteUser(isA(Organization.class), slugsCaptor.capture(), emailCaptor.capture());

        assertThat(slugsCaptor.getAllValues()).hasSize(1);
        assertThat(slugsCaptor.getAllValues().get(0)).contains("A", "B");

        assertThat(emailCaptor.getAllValues()).hasSize(1);
        assertThat(emailCaptor.getAllValues().get(0)).isEqualTo(sampleEmail());
    }


    @Test
    public void testRunNoDefaultGroupsShouldntInvite()
    {
        when(organizationServiceMock.getAll(false)).thenReturn(Collections.EMPTY_LIST);

        processor.run();

        verifyNoMoreInteractions(communicatorMock);
    }


    private List<Organization> sampleOrganizations()
    {
        Organization org = new Organization();
        org.setDefaultGroups(Sets.newHashSet(new Group("A"), new Group("B")));
        return EasyList.build(org);
    }

    private String sampleUsername()
    {
        return "principal";
    }

    private String sampleEmail()
    {
        return "principal@example.com";
    }
}

