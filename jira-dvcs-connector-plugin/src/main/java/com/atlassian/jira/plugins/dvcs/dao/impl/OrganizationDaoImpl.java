package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.dao.OrganizationDao;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.collect.Lists;
import net.java.ao.Query;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.apache.commons.lang.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class OrganizationDaoImpl.
 * 
 */
public class OrganizationDaoImpl implements OrganizationDao
{

	/** The active objects. */
	private final ActiveObjects activeObjects;

	/** The encryptor. */
	private final Encryptor encryptor;

	/**
	 * The Constructor.
	 * 
	 * @param activeObjects
	 *            the active objects
	 * @param encryptor
	 *            the encryptor
	 */
	public OrganizationDaoImpl(ActiveObjects activeObjects, Encryptor encryptor)
	{
		this.activeObjects = activeObjects;
		this.encryptor = encryptor;
	}

	/**
	 * Transform.
	 * 
	 * @param organizationMapping
	 *            the organization mapping
	 * @return the organization
	 */
	protected Organization transform(OrganizationMapping organizationMapping)
	{

		if (organizationMapping == null)
		{
			return null;
		}

		Credential credential = new Credential(organizationMapping.getAdminUsername(),
				organizationMapping.getAdminPassword(), organizationMapping.getAccessToken());

		Organization organization = new Organization(organizationMapping.getID(), organizationMapping.getHostUrl(),
				organizationMapping.getName(), organizationMapping.getDvcsType(),
				organizationMapping.isAutolinkNewRepos(), credential);
		organization.setAutoInviteNewUsers(organizationMapping.isAutoInviteNewUsers());
		organization.setOrganizationUrl(createOrganizationUrl(organizationMapping));
		return organization;
	}

	private String createOrganizationUrl(OrganizationMapping organizationMapping)
	{
		String hostUrl = organizationMapping.getHostUrl();
		// normalize
		if (hostUrl != null && hostUrl.endsWith("/")) {
			hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
		}
		return hostUrl + "/" + organizationMapping.getName();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Organization> getAll()
	{
		final List<OrganizationMapping> organizationMappings = activeObjects
				.executeInTransaction(new TransactionCallback<List<OrganizationMapping>>()
				{

					@Override
					public List<OrganizationMapping> doInTransaction()
					{
						return Lists.newArrayList(activeObjects.find(OrganizationMapping.class, Query.select().order(OrganizationMapping.NAME)));
					}
				});

        return transformCollection(organizationMappings);
	}
	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Organization> getAllByType(String type)
	{
		Query query = Query.select().where(OrganizationMapping.DVCS_TYPE + " = ? ", type).order(OrganizationMapping.NAME);
		OrganizationMapping[] found = activeObjects.find(OrganizationMapping.class, query);
		return transformCollection(Lists.newArrayList(found));
	}

	/**
	 * Transform collection.
	 * 
	 * @param organizationMappings
	 *            the organization mappings
	 * @return the list< organization>
	 */
	@SuppressWarnings("unchecked")
	private List<Organization> transformCollection(final List<OrganizationMapping> organizationMappings)
	{
		return (List<Organization>) CollectionUtils.collect(organizationMappings, new Transformer()
		{
			@Override
			public Object transform(Object o)
			{
				return OrganizationDaoImpl.this.transform((OrganizationMapping) o);
			}
		});
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Organization get(final int organizationId)
	{
		OrganizationMapping organizationMapping = activeObjects
				.executeInTransaction(new TransactionCallback<OrganizationMapping>()
				{
					@Override
					public OrganizationMapping doInTransaction()
					{
						return activeObjects.get(OrganizationMapping.class, organizationId);
					}
				});

		return transform(organizationMapping);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Organization getByHostAndName(final String hostUrl, final String name)
	{
		OrganizationMapping organizationMapping = activeObjects
				.executeInTransaction(new TransactionCallback<OrganizationMapping>()
				{
					@Override
					public OrganizationMapping doInTransaction()
					{
						Query query = Query.select().where(
								OrganizationMapping.HOST_URL + " = ? AND " + OrganizationMapping.NAME + " = ? ",
								hostUrl, name).order(OrganizationMapping.NAME);

						final OrganizationMapping[] organizationMappings = activeObjects.find(
								OrganizationMapping.class, query);
						return organizationMappings.length != 0 ? organizationMappings[0] : null;
					}
				});

		return transform(organizationMapping);

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Organization save(final Organization organization)
	{

		final OrganizationMapping organizationMapping = activeObjects
				.executeInTransaction(new TransactionCallback<OrganizationMapping>()
				{

					@Override
					public OrganizationMapping doInTransaction()
					{

						String adminPassword = organization.getCredential().getAdminPassword();

						OrganizationMapping om = null;

						if (organization.getId() == 0)
						{
							// encrypt password for new organization
							if (adminPassword != null)
							{
								adminPassword = encryptor.encrypt(organization.getCredential().getAdminPassword(),
										organization.getName(), organization.getHostUrl());
							}
							
							final Map<String, Object> map = new HashMap<String, Object>();
							map.put(OrganizationMapping.HOST_URL, organization.getHostUrl());
							map.put(OrganizationMapping.NAME, organization.getName());
							map.put(OrganizationMapping.DVCS_TYPE, organization.getDvcsType());
							map.put(OrganizationMapping.AUTOLINK_NEW_REPOS, organization.isAutolinkNewRepos());
							map.put(OrganizationMapping.ADMIN_USERNAME, organization.getCredential().getAdminUsername());
							map.put(OrganizationMapping.ADMIN_PASSWORD, adminPassword);
							map.put(OrganizationMapping.ACCESS_TOKEN, organization.getCredential().getAccessToken());
							map.put(OrganizationMapping.AUTO_INVITE_NEW_USERS, organization.isAutoInviteNewUsers());

							om = activeObjects.create(OrganizationMapping.class, map);
						} else
						{
							om = activeObjects.get(OrganizationMapping.class, organization.getId());

							om.setHostUrl(organization.getHostUrl());
							om.setName(organization.getName());
							om.setDvcsType(organization.getDvcsType());
							om.setAutolinkNewRepos(organization.isAutolinkNewRepos());
							om.setAdminUsername(organization.getCredential().getAdminUsername());
							om.setAdminPassword(adminPassword);
							om.setAccessToken(organization.getCredential().getAccessToken());
							om.setAutoInviteNewUsers(organization.isAutoInviteNewUsers());

							om.save();
						}

						return om;
					}
				});

		activeObjects.flush(organizationMapping);

		return transform(organizationMapping);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void remove(int organizationId)
	{
		activeObjects.delete(activeObjects.get(OrganizationMapping.class, organizationId));

	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void updateCredentials(int organizationId, String username, String plaintextPassword, String accessToken)
	{

		final OrganizationMapping organization = activeObjects.get(OrganizationMapping.class, organizationId);

		// username
		if (StringUtils.isNotBlank(username))
		{
			organization.setAdminUsername(username);
		}
		
		// password
		if (StringUtils.isNotBlank(plaintextPassword))
		{
			organization.setAdminPassword(encryptor.encrypt(plaintextPassword, organization.getName(),
					organization.getHostUrl()));
		}

		// access token
		if (StringUtils.isNotBlank(accessToken))
		{
			organization.setAccessToken(accessToken);
		}

		activeObjects.executeInTransaction(new TransactionCallback<Void>()
		{
			@Override
			public Void doInTransaction()
			{
				organization.save();
				return null;
			}

		});

	}

	
	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Organization> getAllByIds(Collection<Integer> ids)
	{
		OrganizationMapping[] orgMappings = activeObjects.get(OrganizationMapping.class, ids.toArray(new Integer[] {}));
		return transformCollection(Arrays.asList(orgMappings));
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public List<Organization> getAutoInvitionOrganizations()
	{
		Query query = Query.select().where(OrganizationMapping.AUTO_INVITE_NEW_USERS, Boolean.TRUE).order(OrganizationMapping.NAME);
		OrganizationMapping[] orgMappings = activeObjects.find(OrganizationMapping.class, query);
		return transformCollection(Arrays.asList(orgMappings));
	}

}