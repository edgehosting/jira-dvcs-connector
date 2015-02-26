package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.dao.impl.OrganizationDaoImpl;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.service.InvalidOrganizationManager;
import com.atlassian.sal.api.transaction.TransactionCallback;
import net.java.ao.Query;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.entry;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

public class OrganizationDaoTest
{
    @Mock
    private ActiveObjects activeObjectsMock;
    @Mock
    private OrganizationMapping organizationMappingMock;
    @Mock
    private OrganizationMapping organizationMapping;
    @Captor
    private ArgumentCaptor<Map<String, Object>> mapArgumentCaptor;

    private OrganizationDao organizationDao;

	@BeforeMethod
	public void initializeMocksAndOrganizationDAO()
	{
        MockitoAnnotations.initMocks(this);
		organizationDao = new OrganizationDaoImpl(activeObjectsMock, mock(Encryptor.class), mock(InvalidOrganizationManager.class));
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
        when(activeObjectsMock.create(eq(OrganizationMapping.class), isA(Map.class))).thenReturn(
                organizationMapping);
        when(activeObjectsMock.find(eq(OrganizationMapping.class), anyString(), any())).thenReturn(
                new OrganizationMapping[] { organizationMapping });

        organizationDao.save(organization);

        verify(activeObjectsMock).create(eq(OrganizationMapping.class), mapArgumentCaptor.capture());

        assertThat(mapArgumentCaptor.getValue()).contains(entry(OrganizationMapping.ACCESS_TOKEN, "accessToken"),
                entry(OrganizationMapping.ADMIN_USERNAME, "adminUserName"),
                entry(OrganizationMapping.DVCS_TYPE, "bitbucket"),
                entry(OrganizationMapping.HOST_URL, "organizationHostUrl"),
                entry(OrganizationMapping.NAME, "organizationName"),

                entry(OrganizationMapping.AUTOLINK_NEW_REPOS, true),

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

        verify(organizationMappingMock).setAccessToken(eq("accessToken"));
        verify(organizationMappingMock).setAdminUsername(eq("adminUserName"));
        verify(organizationMappingMock).setDvcsType     (eq("bitbucket"));
        verify(organizationMappingMock).setHostUrl      (eq("organizationHostUrl"));
        verify(organizationMappingMock).setName         (eq("organizationName"));

        verify(organizationMappingMock).setAutolinkNewRepos  (eq(true));

        verify(organizationMappingMock).setAdminPassword(eq((String) null));

        verify(organizationMappingMock).save();
    }

    private Organization createSampleOrganization()
    {
        Organization organization = new Organization();

        organization.setId(0); // to be sure that the entity object will be saved for the first time

        organization.setHostUrl ("organizationHostUrl");
        organization.setName    ("organizationName");
        organization.setDvcsType("bitbucket");

        organization.setAutolinkNewRepos  (true);

        Credential organizationCredential = new Credential(null, null, "accessToken", "adminUserName", null);
        organization.setCredential(organizationCredential);

        return organization;
    }
    
    @Test
    public void testGetByHostAndName()
    {
        when(activeObjectsMock.executeInTransaction(isA(TransactionCallback.class))).thenAnswer(new Answer<Object>()
        {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable
            {
                return ((TransactionCallback) invocationOnMock.getArguments()[0]).doInTransaction();
            }
        });

        when(activeObjectsMock.find(eq(OrganizationMapping.class), any(Query.class))).thenReturn(
                new OrganizationMapping[] {
                        organizationMappingMock, organizationMapping
                });
        when(organizationMapping.getName()).thenReturn("test");

        assertNotNull(organizationDao.getByHostAndName("https://bitbucket.org", "test"));
        assertNotNull(organizationDao.getByHostAndName("https://bitbucket.org", "teSt"));
        assertNotNull(organizationDao.getByHostAndName("https://bitbucket.org", "TEST"));
        assertNotNull(organizationDao.getByHostAndName("https://bitbucket.org", "Test"));
        assertNull(organizationDao.getByHostAndName("https://bitbucket.org", "nontest"));
        assertNull(organizationDao.getByHostAndName("https://bitbucket.org", null));
    }
}
