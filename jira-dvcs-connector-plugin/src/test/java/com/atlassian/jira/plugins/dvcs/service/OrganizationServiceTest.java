package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;

/**
 * The Class OrganizationServiceTest.
 */
public class OrganizationServiceTest
{
	@Mock
	private RepositoryService repositoryService;

    @Mock
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Mock
    private OrganizationDao organizationDao;

    @Mock
    private DvcsCommunicator bitbucketCommunicator;

	// tested object
    @InjectMocks
	private OrganizationServiceImpl organizationService;

    /**
     * The Constructor.
     */
    public OrganizationServiceTest()
    {
        super();
    }

	@BeforeMethod
	public void setup()
	{
		MockitoAnnotations.initMocks(this);
	}

    @Test
    public void testCreateOrganization()
    {
        Organization sampleOrganization = createSampleOrganization();

        Mockito.when(organizationDao.getByHostAndName("https://bitbucket.org", "doesnotmatter")).thenReturn(null);
        Mockito.when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
        Mockito.when(organizationDao.save(sampleOrganization)).thenReturn(sampleOrganization);

        Organization saved = organizationService.save(sampleOrganization);

        assertThat(saved).isSameAs(sampleOrganization);

        Mockito.verify(repositoryService, Mockito.times(1)).syncRepositoryList(sampleOrganization, false);
    }

    @Test
    public void testGetAllByType()
    {
        Organization sampleOrganization = createSampleOrganization();
        ArrayList<Organization> list = new ArrayList<Organization>();
        list.add(sampleOrganization);

        Mockito.when(organizationDao.getAllByType("bitbucket")).thenReturn(list);

        List<Organization> all = organizationService.getAll(false, "bitbucket");

        assertThat(all.get(0)).isSameAs(sampleOrganization);

        Mockito.verify(organizationDao).getAllByType("bitbucket");

    }

    @Test
    public void testGetAllByTypeLoadRepositories()
    {
        Organization sampleOrganization = createSampleOrganization();
        ArrayList<Organization> list = new ArrayList<Organization>();
        list.add(sampleOrganization);

        Mockito.when(organizationDao.getAllByType("bitbucket")).thenReturn(list);
        Mockito.when(repositoryService.getAllByOrganization(0)).thenReturn(new ArrayList<Repository>());

        organizationService.getAll(true, "bitbucket");

        //
        Mockito.verify(organizationDao).getAllByType("bitbucket");
        Mockito.verify(repositoryService).getAllByOrganization(0);

    }

    @Test
    public void testEnableAutolinkNewRepos()
    {
        Organization sampleOrganization = createSampleOrganization();
        Mockito.when(organizationDao.get(0)).thenReturn(sampleOrganization);

        organizationService.enableAutolinkNewRepos(0, true);

        //
        Mockito.verify(organizationDao).save(Mockito.argThat(new ArgumentMatcher<Organization>()
        {
            @Override
            public boolean matches(Object argument)
            {
                Organization savingOrganization = (Organization) argument;
                return savingOrganization.isAutolinkNewRepos();
            }

        }));
    }

    @Test
    public void testDisableAutolinkNewRepos()
    {
        Organization sampleOrganization = createSampleOrganization();
        Mockito.when(organizationDao.get(0)).thenReturn(sampleOrganization);

        organizationService.enableAutolinkNewRepos(0, false);

        //
        Mockito.verify(organizationDao).save(Mockito.argThat(new ArgumentMatcher<Organization>()
        {
            @Override
            public boolean matches(Object argument)
            {
                Organization savingOrganization = (Organization) argument;
                return !savingOrganization.isAutolinkNewRepos();
            }
        }));
    }

    @Test
    public void testUpdateCredentialsAccessToken()
    {
        Organization sampleOrganization = createSampleOrganization();
        Mockito.when(organizationDao.get(0)).thenReturn(sampleOrganization);
        Mockito.when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);

        organizationService.updateCredentialsAccessToken(0, "doesnotmatter_AT");

        Mockito.verify(organizationDao).save(sampleOrganization);
        assertThat(sampleOrganization.getCredential().getAccessToken().equals("doesnotmatter_AT"));
    }

    private Organization createSampleOrganization()
    {
        Organization organization = new Organization();
        organization.setDvcsType("bitbucket");
        organization.setHostUrl("https://bitbucket.org");
        organization.setName("doesnotmatter");
        organization.setCredential(new Credential(null, null, null, "doesnotmatter_u", "doesnotmatter_p"));
        return organization;
    }

}
