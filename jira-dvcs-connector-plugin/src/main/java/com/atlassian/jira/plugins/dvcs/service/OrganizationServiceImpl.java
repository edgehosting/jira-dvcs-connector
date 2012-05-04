package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

public class OrganizationServiceImpl implements OrganizationService
{

    private final OrganizationDao organizationDao;
    private final RepositoryDao repositoryDao;
    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;

    public OrganizationServiceImpl(DvcsCommunicatorProvider dvcsCommunicatorProvider, OrganizationDao organizationDao, RepositoryDao repositoryDao)
    {
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
        this.organizationDao = organizationDao;
        this.repositoryDao = repositoryDao;
    }

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        return dvcsCommunicatorProvider.getAccountInfo(hostUrl, accountName);
    }

    @Override
    public List<Organization> getAll()
    {
        final List<Organization> organizations = organizationDao.getAll();

        CollectionUtils.transform(organizations, new Transformer()
        {
            @Override
            public Object transform(Object o)
            {
                Organization organization = (Organization) o;
                final List<Repository> repositories = repositoryDao.getAllByOrganization(organization.getId());
                organization.setRepositories(repositories.toArray(new Repository[]{}));
                return organization;
            }
        });
        return organizations;
    }

    @Override
    public Organization get(int organizationId)
    {
        final Organization organization = organizationDao.get(organizationId);

        // todo: ozaj array?  nie radsej list?
        final List<Repository> repositoryList = repositoryDao.getAllByOrganization(organizationId);
        organization.setRepositories((Repository[]) repositoryList.toArray());

        return organization;
    }

    @Override
    public Organization save(Organization organization)
    {
        return organizationDao.save(organization);
    }

    @Override
    public void remove(int organizationId)
    {
    }

	@Override
	public void updateCredentials(int organizationId, String plaintextPassword)
	{
		
	}
    
}
