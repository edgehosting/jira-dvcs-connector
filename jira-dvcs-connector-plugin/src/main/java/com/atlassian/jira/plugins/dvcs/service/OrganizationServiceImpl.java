package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
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
    private RepositoryDao repositoryDao;
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
                organization.setRepositories((Repository[]) repositories.toArray());
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
}
