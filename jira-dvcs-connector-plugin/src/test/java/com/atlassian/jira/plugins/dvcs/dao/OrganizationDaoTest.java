package com.atlassian.jira.plugins.dvcs.dao;


import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.dao.impl.OrganizationDaoImpl;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.sal.api.transaction.TransactionCallback;

import static org.fest.assertions.api.Assertions.*;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class OrganizationDaoTest {

    @Mock
    private ActiveObjects activeObjectsMock;

    @Mock
    private OrganizationMapping organizationMappingMock;

    @Captor
    private ArgumentCaptor<Map<String, Object>> mapArgumentCaptor;


    private OrganizationDao organizationDao;


	@Before
	public void initializeOrganizationDAO()
	{
		organizationDao = new OrganizationDaoImpl(activeObjectsMock, mock(Encryptor.class));
	}


    @SuppressWarnings("unchecked")
    @Test
    public void savingNewOrganizationModelObject_ShouldDoProperTransformation_ToOrganizationMappingEntity()
    {
        Organization organization = createSampleOrganization();

		when(activeObjectsMock.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
            }
        });

        organizationDao.save(organization);

        verify(activeObjectsMock).create(eq(OrganizationMapping.class), mapArgumentCaptor.capture());

        assertThat(mapArgumentCaptor.getValue()).contains(entry(OrganizationMapping.ACCESS_TOKEN,   "accessToken"),
                                                          entry(OrganizationMapping.ADMIN_USERNAME, "adminUserName"),
                                                          entry(OrganizationMapping.DVCS_TYPE,      "bitbucket"),
                                                          entry(OrganizationMapping.HOST_URL,       "organizationHostUrl"),
                                                          entry(OrganizationMapping.NAME,           "organizationName"),

                                                          entry(OrganizationMapping.AUTOLINK_NEW_REPOS,    true),
                                                          entry(OrganizationMapping.AUTO_INVITE_NEW_USERS, true),

                                                          entry(OrganizationMapping.ADMIN_PASSWORD, null));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void updatingOrganizationModelObject_ShouldCallSettersAndSaveMethods_OnOrganizationMappingEntity()
    {
        final int ORGANIZATION_ID = 123;

        Organization organization = createSampleOrganization();
        organization.setId(ORGANIZATION_ID);

		when(activeObjectsMock.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
            }
        });

        when(activeObjectsMock.get(eq(OrganizationMapping.class), eq(ORGANIZATION_ID))).thenReturn(organizationMappingMock);

        organizationDao.save(organization);

        verify(organizationMappingMock).setAccessToken  (eq("accessToken"));
        verify(organizationMappingMock).setAdminUsername(eq("adminUserName"));
        verify(organizationMappingMock).setDvcsType     (eq("bitbucket"));
        verify(organizationMappingMock).setHostUrl      (eq("organizationHostUrl"));
        verify(organizationMappingMock).setName         (eq("organizationName"));

        verify(organizationMappingMock).setAutoInviteNewUsers(eq(true));
        verify(organizationMappingMock).setAutolinkNewRepos  (eq(true));

        verify(organizationMappingMock).setAdminPassword(eq((String) null));

        verify(organizationMappingMock).save();
    }


    private static Organization createSampleOrganization()
    {
        Organization organization = new Organization();

        organization.setId(0); // to be sure that the entity object will be saved for the first time

        organization.setHostUrl ("organizationHostUrl");
        organization.setName    ("organizationName");
        organization.setDvcsType("bitbucket");

        organization.setAutolinkNewRepos  (true);
        organization.setAutoInviteNewUsers(true);

        Credential organizationCredential = new Credential("adminUserName", null, "accessToken");
        organization.setCredential(organizationCredential);

        return organization;
    }
}
