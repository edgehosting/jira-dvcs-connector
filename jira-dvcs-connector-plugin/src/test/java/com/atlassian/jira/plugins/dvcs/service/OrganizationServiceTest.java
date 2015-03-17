package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import com.google.common.collect.ImmutableList;
import org.mockito.ArgumentMatcher;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.List;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


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

    @Mock
    private Repository repository;

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

        when(organizationDao.getByHostAndName("https://bitbucket.org", "doesnotmatter")).thenReturn(null);
        when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);
        when(organizationDao.save(sampleOrganization)).thenReturn(sampleOrganization);

        Organization saved = organizationService.save(sampleOrganization);

        assertThat(saved).isSameAs(sampleOrganization);

        verify(repositoryService, Mockito.times(1)).syncRepositoryList(sampleOrganization, false);
    }

    @Test
    public void testGetAllWithRepos()
    {
        Organization sampleOrganization = createSampleOrganization();
        List<Organization> list = ImmutableList.of(sampleOrganization);

        when(organizationDao.getAll()).thenReturn(list);
        when(repositoryService.getAllByOrganization(0)).thenReturn(ImmutableList.of(repository));

        List<Organization> all = organizationService.getAll(true);

        verify(organizationDao).getAll();
        assertOrgWithRepos(all, sampleOrganization);
    }

    @Test
    public void testGetWithRepos()
    {
        final int ORG_ID = 1;
        Organization sampleOrganization = createSampleOrganization();
        when(organizationDao.get(ORG_ID)).thenReturn(sampleOrganization);
        when(repositoryService.getAllByOrganization(0)).thenReturn(ImmutableList.of(repository));

        Organization orgWithRepos = organizationService.get(ORG_ID, true);

        verify(organizationDao).get(ORG_ID);
        assertOrgWithRepos(ImmutableList.of(orgWithRepos), sampleOrganization);
    }

    @Test
    public void testGetAllByType()
    {
        Organization sampleOrganization = createSampleOrganization();
        List<Organization> list = ImmutableList.of(sampleOrganization);

        when(organizationDao.getAllByType("bitbucket")).thenReturn(list);

        List<Organization> all = organizationService.getAll(false, "bitbucket");

        assertThat(all.get(0)).isSameAs(sampleOrganization);

        verify(organizationDao).getAllByType("bitbucket");
    }

    @Test
    public void testGetAllByTypeLoadRepositories()
    {
        Organization sampleOrganization = createSampleOrganization();
        List<Organization> list = ImmutableList.of(sampleOrganization);

        when(organizationDao.getAllByType("bitbucket")).thenReturn(list);
        when(repositoryService.getAllByOrganization(0)).thenReturn(ImmutableList.of(repository));

        List<Organization> orgWithRepos = organizationService.getAll(true, "bitbucket");

        verify(organizationDao).getAllByType("bitbucket");
        verify(repositoryService).getAllByOrganization(0);

        assertOrgWithRepos(orgWithRepos, sampleOrganization);
    }

    @Test
    public void testEnableAutolinkNewRepos()
    {
        Organization sampleOrganization = createSampleOrganization();
        when(organizationDao.get(0)).thenReturn(sampleOrganization);

        organizationService.enableAutolinkNewRepos(0, true);

        verify(organizationDao).save(Mockito.argThat(new ArgumentMatcher<Organization>()
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
        when(organizationDao.get(0)).thenReturn(sampleOrganization);

        organizationService.enableAutolinkNewRepos(0, false);

        verify(organizationDao).save(Mockito.argThat(new ArgumentMatcher<Organization>()
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
        when(organizationDao.get(0)).thenReturn(sampleOrganization);
        when(dvcsCommunicatorProvider.getCommunicator("bitbucket")).thenReturn(bitbucketCommunicator);

        organizationService.updateCredentialsAccessToken(0, "doesnotmatter_AT");

        verify(organizationDao).save(sampleOrganization);
        assertThat(sampleOrganization.getCredential().getAccessToken().equals("doesnotmatter_AT"));
    }

    private void assertOrgWithRepos(List<Organization> orgWithRepos, Organization originalOrg) {
        // make sure the Organization with repositories was cloned
        assertThat(orgWithRepos.size()).isEqualTo(1);
        assertThat(orgWithRepos.get(0).getName()).isEqualTo(originalOrg.getName());
        assertThat(orgWithRepos.get(0).getDvcsType()).isEqualTo(originalOrg.getDvcsType());
        assertThat(orgWithRepos.get(0).getHostUrl()).isEqualTo(originalOrg.getHostUrl());

        assertThat(orgWithRepos.get(0).getRepositories().size()).isEqualTo(1);
        assertThat(orgWithRepos.get(0).getRepositories().get(0)).isSameAs(repository);

        // make sure original organization was not changed
        assertThat(originalOrg.getRepositories()).isNull();
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
