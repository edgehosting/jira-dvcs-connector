package com.atlassian.jira.plugins.dvcs.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.exception.InvalidCredentialsException;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

public class OrganizationServiceImpl implements OrganizationService
{

	private final OrganizationDao organizationDao;
	private final DvcsCommunicatorProvider dvcsCommunicatorProvider;
	private final RepositoryService repositoryService;


	public OrganizationServiceImpl(OrganizationDao organizationDao, DvcsCommunicatorProvider dvcsCommunicatorProvider,
        RepositoryService repositoryService)
    {
        this.organizationDao = organizationDao;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
        this.repositoryService = repositoryService;
    }

    @Override
	public AccountInfo getAccountInfo(String hostUrl, String accountName)
	{
		return dvcsCommunicatorProvider.getAccountInfo(hostUrl, accountName);
	}

    @Override
    public List<Organization> getAll(boolean loadRepositories)
    {
        List<Organization> organizations = organizationDao.getAll();

        if (loadRepositories)
        {
            for (Organization organization : organizations)
            {
                List<Repository> repositories = repositoryService.getAllByOrganization(organization.getId());
                organization.setRepositories(repositories);
            }
        }
        return organizations;
    }
	
	@Override
	public List<Organization> getAll(boolean loadRepositories, String type)
	{
		final List<Organization> organizations = organizationDao.getAllByType(type);

		if (loadRepositories)
		{
            for (Organization organization : organizations)
            {
                List<Repository> repositories = repositoryService.getAllByOrganization(organization.getId());

                organization.setRepositories(repositories);
            }
		}

		return organizations;
	}
	

	@Override
	public Organization get(int organizationId, boolean loadRepositories)
	{
		final Organization organization = organizationDao.get(organizationId);

		if (loadRepositories)
		{
			final List<Repository> repositories = repositoryService.getAllByOrganization(organizationId);
			organization.setRepositories(repositories);
		}

		return organization;
	}

	@Override
	public Organization save(Organization organization)
	{
		Organization org = organizationDao.getByHostAndName(organization.getHostUrl(), organization.getName());
		if (org != null) {
			// nop;
			// we've already have this organization, don't save another one
			//
			return org;
		}
		
		//
		// it's brand new organization. save it.
		//
		checkCredentials(organization);
		//
		org = organizationDao.save(organization);

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
	public void updateCredentials(int organizationId, String username, String plaintextPassword)
	{
		// Check credentials
		// create organization with plain credentials as we need all data like url, etc
		//
		Organization organization = organizationDao.get(organizationId);
		organization.setCredential(new Credential(username, plaintextPassword, null));
		checkCredentials(organization);
		
		organizationDao.updateCredentials(organizationId, username, plaintextPassword, null);
	}

	@Override
	public void updateCredentialsAccessToken(int organizationId, String accessToken)
	{
		// Check credentials
		// create organization with plain credentials as we need all data like url, etc
		//
		Organization organization = organizationDao.get(organizationId);
		organization.setCredential(new Credential(null, null, accessToken));
		checkCredentials(organization);
		//
		
		organizationDao.updateCredentials(organizationId, null, null, accessToken);

	}

	@Override
	public void enableAutolinkNewRepos(int orgId, boolean autolink)
	{
		final Organization organization = organizationDao.get(orgId);
		if (organization != null)
		{
			organization.setAutolinkNewRepos(autolink);
			organizationDao.save(organization);
		}
	}

	
	@Override
	public void enableSmartcommitsOnNewRepos(int id, boolean enabled)
	{
		final Organization organization = organizationDao.get(id);
		if (organization != null)
		{
			organization.setSmartcommitsOnNewRepos(enabled);
			organizationDao.save(organization);
		}
		
	}

	@Override
	public List<Organization> getAutoInvitionOrganizations()
	{
		return organizationDao.getAutoInvitionOrganizations();
	}

	@Override
	public List<Organization> getAllByIds(Collection<Integer> ids)
	{
		if (CollectionUtils.isNotEmpty(ids)) {
			return organizationDao.getAllByIds(ids);
		} else {
			return Collections.emptyList();
		}
		
	}

	@Override
	public void checkCredentials(Organization forOrganizationWithPlainCredentials) throws InvalidCredentialsException
	{
		String bitbucketDvcsType = "bitbucket";

		// validate just bitbucket credentials for now
		if (bitbucketDvcsType.equalsIgnoreCase(forOrganizationWithPlainCredentials.getDvcsType())) {
			DvcsCommunicator bitbucket = dvcsCommunicatorProvider.getCommunicator(bitbucketDvcsType);
			boolean valid = bitbucket.validateCredentials(forOrganizationWithPlainCredentials);
			if (!valid) {
				throw new InvalidCredentialsException("Incorrect password");
			}
		}
		
	}

	@Override
	public void setDefaultGroupsSlugs(int orgId, Collection<String> groupsSlugs)
	{
		organizationDao.setDefaultGroupsSlugs(orgId, groupsSlugs);
	}


}
