package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import java.util.List;

public class OrganizationServiceImpl implements OrganizationService
{

    private OrganizationDao organizationDao;
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;
    private RepositoryService repositoryService;

    public OrganizationServiceImpl()
    {
    }

    public void setOrganizationDao(OrganizationDao organizationDao)
    {
        this.organizationDao = organizationDao;
    }

    public void setDvcsCommunicatorProvider(DvcsCommunicatorProvider dvcsCommunicatorProvider)
    {
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
    }

    public void setRepositoryService(RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        return dvcsCommunicatorProvider.getAccountInfo(hostUrl, accountName);
    }

    @Override
    public List<Organization> getAll(final boolean loadRepositories)
    {
        final List<Organization> organizations = organizationDao.getAll();

        if (loadRepositories)
        {
            CollectionUtils.transform(organizations, new Transformer()
            {
                @Override
                public Object transform(Object o)
                {
                    Organization organization = (Organization) o;
                    final List<Repository> repositories = repositoryService.getAllByOrganization(organization.getId(), false);
                    organization.setRepositories(repositories);
                    return organization;
                }
            });
        }

        return organizations;
    }

    @Override
    public Organization get(int organizationId, boolean loadRepositories)
    {
        final Organization organization = organizationDao.get(organizationId);

        if (loadRepositories)
        {
            final List<Repository> repositories = repositoryService.getAllByOrganization(organizationId, false);
            organization.setRepositories(repositories);
        }

        return organization;
    }


    @Override
    public Organization save(Organization organization)
    {
        // todo: uz taku mame. co s Login a Passwd. mozno to treba ziastovat uz skor a na UI
        Organization org = organizationDao.getByHostAndName(organization.getHostUrl(), organization.getName());

        // it's brand new organization. save it.
        if (org == null)
        {
            org = organizationDao.save(organization);
        }

        // sync repository list
        repositoryService.syncRepositoryList(org);

        return org;
    }

    @Override
    public void remove(int organizationId)
    {
        repositoryService.removeAllInOrganization(organizationId);
    	organizationDao.remove(organizationId);
    }

	@Override
	public void updateCredentials(int organizationId, String plaintextPassword)
	{
		// TODO check if new credential works
		organizationDao.updateCredentials(organizationId, plaintextPassword, null);
	}

	@Override
	public void updateCredentialsAccessToken(int organizationId,
			String accessToken) {

		// TODO check if new credential works
		organizationDao.updateCredentials(organizationId, null, accessToken);
		
	}

	@Override
	public void enableAutolinkNewRepos(int orgId, boolean autolink) {
        final Organization organization = organizationDao.get(orgId);
        if (organization != null)
        {
            organization.setAutolinkNewRepos(autolink);
            organizationDao.save(organization);
        }

    }
	
    
}
