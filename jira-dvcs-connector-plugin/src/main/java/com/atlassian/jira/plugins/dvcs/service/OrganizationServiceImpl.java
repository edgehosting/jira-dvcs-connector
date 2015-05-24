package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Group;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Component
public class OrganizationServiceImpl implements OrganizationService
{
    private static final Logger log = LoggerFactory.getLogger(OrganizationServiceImpl.class);

    @Autowired
    private OrganizationDao organizationDao;

    @Autowired
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Autowired
    private RepositoryService repositoryService;

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName)
    {
        return getAccountInfo(hostUrl, accountName, null);
    }

    @Override
    public AccountInfo getAccountInfo(String hostUrl, String accountName, String dvcsType)
    {
        return dvcsCommunicatorProvider.getAccountInfo(hostUrl, accountName, dvcsType);
    }

    @Override
    public List<Organization> getAll(boolean loadRepositories)
    {
        List<Organization> organizations = organizationDao.getAll();

        if (loadRepositories)
        {
            for (Organization organization : organizations)
            {
                retrieveRepositories(organization);
            }
        }
        return organizations;
    }

    @Override
    public List<Organization> getAll(boolean loadRepositories, String type)
    {
        List<Organization> organizations = organizationDao.getAllByType(type);

        if (loadRepositories)
        {
            for (Organization organization : organizations)
            {
                retrieveRepositories(organization);
            }
        }

        return organizations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getAllCount()
    {
        return organizationDao.getAllCount();
    }

    @Override
    public Organization get(int organizationId, boolean loadRepositories)
    {
        Organization organization = organizationDao.get(organizationId);

        if (loadRepositories && organization != null)
        {
            retrieveRepositories(organization);
        }

        return organization;
    }

    @Override
    public Organization save(Organization organization)
    {
        Organization org = organizationDao.getByHostAndName(organization.getHostUrl(), organization.getName());
        if (org != null)
        {
            // nop;
            // we've already have this organization, don't save another one
            return org;
        }

        //
        // it's brand new organization. save it.
        //
        org = organizationDao.save(organization);

        // sync repository list
        repositoryService.syncRepositoryList(org, false);

        return org;
    }

    @Override
    public void remove(int organizationId)
    {
        long startTime = System.currentTimeMillis();
        List<Repository> repositoriesToDelete = repositoryService.getAllByOrganization(organizationId, true);
        repositoryService.removeRepositories(repositoriesToDelete);
        organizationDao.remove(organizationId);
        repositoryService.removeOrphanRepositories(repositoriesToDelete);
        log.debug("Organization {} was deleted in {} ms", organizationId, System.currentTimeMillis() - startTime);
    }

    @Override
    public void updateCredentials(int organizationId, Credential credential)
    {
        Organization organization = organizationDao.get(organizationId);
        if (organization != null)
        {
            organization.setCredential(credential);
            organizationDao.save(organization);
        }
    }

    @Override
    public void updateCredentialsAccessToken(int organizationId, String accessToken)
    {
        Organization organization = organizationDao.get(organizationId);
        if (organization != null)
        {
            organization.getCredential().setAccessToken(accessToken);
            organizationDao.save(organization);
        }
    }

    @Override
    public void enableAutolinkNewRepos(int orgId, boolean autolink)
    {
        Organization organization = organizationDao.get(orgId);
        if (organization != null)
        {
            organization.setAutolinkNewRepos(autolink);
            organizationDao.save(organization);
        }
    }

    @Override
    public void enableSmartcommitsOnNewRepos(int id, boolean enabled)
    {
        Organization organization = organizationDao.get(id);
        if (organization != null)
        {
            organization.setSmartcommitsOnNewRepos(enabled);
            organizationDao.save(organization);
        }

    }

    @Override
    public List<Organization> getAllByIds(Collection<Integer> ids)
    {
        if (CollectionUtils.isNotEmpty(ids))
        {
            return organizationDao.getAllByIds(ids);
        }
        else
        {
            return Collections.emptyList();
        }
    }

    @Override
    public void setDefaultGroupsSlugs(int orgId, Collection<String> groupsSlugs)
    {
        organizationDao.setDefaultGroupsSlugs(orgId, groupsSlugs);
    }

    @Override
    public Organization findIntegratedAccount()
    {
        return organizationDao.findIntegratedAccount();
    }

    @Override
    public Organization getByHostAndName(String hostUrl, String name)
    {
        return organizationDao.getByHostAndName(hostUrl, name);
    }

    @Override
    public DvcsUser getTokenOwner(int organizationId)
    {
        Organization organization = get(organizationId, false);
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(organization.getDvcsType());
        return communicator.getTokenOwner(organization);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Group> getGroupsForOrganization(Organization organization)
    {
        return dvcsCommunicatorProvider.getCommunicator(organization.getDvcsType()).getGroupsForOrganization(organization);
    }

    @Override
    public boolean existsOrganizationWithType(final String... types)
    {
        return organizationDao.existsOrganizationWithType(types);
    }

    private void retrieveRepositories(Organization org)
    {
        List<Repository> repositories = repositoryService.getAllByOrganization(org.getId());
        org.setRepositories(repositories);
    }
}
