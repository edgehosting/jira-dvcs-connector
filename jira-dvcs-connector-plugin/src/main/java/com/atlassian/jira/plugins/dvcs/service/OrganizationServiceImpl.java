package com.atlassian.jira.plugins.dvcs.service;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;

import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.AccountInfo;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
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
					final List<Repository> repositories = repositoryService.getAllByOrganization(organization.getId(),
							false);
					organization.setRepositories(repositories);
					return organization;
				}
			});
		}

		return organizations;
	}
	
	@Override
	public List<Organization> getAll(boolean loadRepositories, String type)
	{
		final List<Organization> organizations = organizationDao.getAllByType(type);

		if (loadRepositories)
		{
			CollectionUtils.transform(organizations, new Transformer()
			{
				@Override
				public Object transform(Object o)
				{
					Organization organization = (Organization) o;
					final List<Repository> repositories = repositoryService.getAllByOrganization(organization.getId(),
							false);
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
		// todo: uz taku mame. co s Login a Passwd. mozno to treba ziastovat uz
		// skor a na UI
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
	public void updateCredentials(int organizationId, String username, String plaintextPassword)
	{
		// TODO check if new credential works
		organizationDao.updateCredentials(organizationId, username, plaintextPassword, null);
	}

	@Override
	public void updateCredentialsAccessToken(int organizationId, String accessToken)
	{

		// TODO check if new credential works
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
	public void enableAutoInviteUsers(int id, boolean autoInviteUsers)
	{
		final Organization organization = organizationDao.get(id);
		if (organization != null)
		{
			organization.setAutoInviteNewUsers(autoInviteUsers);
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
			return Collections.EMPTY_LIST;
		}
		
	}

}
