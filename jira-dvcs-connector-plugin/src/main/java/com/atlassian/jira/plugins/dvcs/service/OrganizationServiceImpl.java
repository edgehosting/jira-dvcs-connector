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

    private final OrganizationDao organizationDao;
    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;
    private final RepositoryService repositoryService;

    public OrganizationServiceImpl(DvcsCommunicatorProvider dvcsCommunicatorProvider, OrganizationDao organizationDao, RepositoryService repositoryService)
    {
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
        this.organizationDao = organizationDao;
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

        // todo: install post commit hook - before sync()  !!! pozor na autoLink

        // start asynchronous changesets synchronization for all repositories in organization
        repositoryService.syncAllInOrganization(org.getId());

        // todo: pri pridavani repo pozriet ci org.autolink -> podla toho sync / postcommit



        return org;
    }

    @Override
    public void remove(int organizationId)
    {
        // todo
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
	public void enableAutolinkNewRepos(int orgId, boolean parseBoolean) {
		// TODO Auto-generated method stub
		
	}
	
    
}
