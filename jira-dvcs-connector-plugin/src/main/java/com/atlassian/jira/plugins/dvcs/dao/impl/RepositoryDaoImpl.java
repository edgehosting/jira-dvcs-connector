package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Transformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.atlassian.sal.api.transaction.TransactionCallback;

public class RepositoryDaoImpl implements RepositoryDao
{

	private static final Logger log = LoggerFactory.getLogger(RepositoryDaoImpl.class);

	private final ActiveObjects activeObjects;
	private final Synchronizer synchronizer;

	public RepositoryDaoImpl(ActiveObjects activeObjects, Synchronizer synchronizer)
	{
		this.activeObjects = activeObjects;
		this.synchronizer = synchronizer;
	}

	protected Repository transform(RepositoryMapping repositoryMapping, OrganizationMapping organizationMapping)
	{
        if (repositoryMapping == null || organizationMapping == null)
        {
            return null;
        }

        log.debug("Repository transformation: [{}] ", repositoryMapping);

		Credential credential = new Credential(organizationMapping.getAdminUsername(),
				organizationMapping.getAdminPassword(), organizationMapping.getAccessToken());
		
		credential.setOauthKey(organizationMapping.getOauthKey());
		credential.setOauthSecret(organizationMapping.getOauthSecret());
		
		Repository repository = new Repository(repositoryMapping.getID(), repositoryMapping.getOrganizationId(),
				organizationMapping.getDvcsType(), repositoryMapping.getSlug(), repositoryMapping.getName(),
                repositoryMapping.getLastCommitDate(),
                repositoryMapping.isLinked(), repositoryMapping.isDeleted(), credential);

		repository.setOrgHostUrl(organizationMapping.getHostUrl());
		repository.setOrgName(organizationMapping.getName());
		repository.setRepositoryUrl(createRepositoryUrl(repositoryMapping, organizationMapping));
		repository.setSmartcommitsEnabled(repositoryMapping.isSmartcommitsEnabled());
		repository.setActivityLastSync(repositoryMapping.getActivityLastSync());
		
		// set sync progress
		repository.setSync((DefaultProgress) synchronizer.getProgress(repository));

		return repository;
	}

	private String createRepositoryUrl(RepositoryMapping repositoryMapping, OrganizationMapping organizationMapping)
	{
		String hostUrl = organizationMapping.getHostUrl();
		// normalize
        if (hostUrl != null && hostUrl.endsWith("/"))
        {
            hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
        }
		return hostUrl + "/" + organizationMapping.getName() + "/" + repositoryMapping.getSlug();
	}

    @SuppressWarnings("unchecked")
	@Override
	public List<Repository> getAllByOrganization(final int organizationId, final boolean includeDeleted)
	{
		List<RepositoryMapping> repositoryMappings = activeObjects
				.executeInTransaction(new TransactionCallback<List<RepositoryMapping>>()
                {
                    @Override
                    public List<RepositoryMapping> doInTransaction()
                    {
                        Query query = Query.select().where(RepositoryMapping.ORGANIZATION_ID + " = ? ", organizationId);
                        if (!includeDeleted)
                        {
                            query = Query.select().where(
                                    RepositoryMapping.ORGANIZATION_ID + " = ? AND " + RepositoryMapping.DELETED
                                            + " = ? ", organizationId, Boolean.FALSE);
                        }
                        query.order(RepositoryMapping.NAME);

                        final RepositoryMapping[] rms = activeObjects.find(RepositoryMapping.class, query);
                        return Arrays.asList(rms);
                    }
                });

		final OrganizationMapping organizationMapping = getOrganizationMapping(organizationId);

        return (List<Repository>) CollectionUtils.collect(repositoryMappings, new Transformer() {

            @Override
            public Object transform(Object input)
            {
                RepositoryMapping repositoryMapping = (RepositoryMapping) input;
                
                return RepositoryDaoImpl.this.transform(repositoryMapping, organizationMapping);
            }
        });
	}

	@Override
	public List<Repository> getAll(final boolean includeDeleted)
	{

		List<RepositoryMapping> repositoryMappings = activeObjects
				.executeInTransaction(new TransactionCallback<List<RepositoryMapping>>()
				{
					@Override
					public List<RepositoryMapping> doInTransaction()
					{
						Query select = Query.select();
						if (!includeDeleted)
						{
							select = select.where(RepositoryMapping.DELETED + " = ? ", Boolean.FALSE);
						}
                        select.order(RepositoryMapping.NAME);

						final RepositoryMapping[] repos = activeObjects.find(RepositoryMapping.class, select);
						return Arrays.asList(repos);
					}
				});

		// fill organizations for repositories as we need them for
		// transformations
		final Map<Integer, OrganizationMapping> idToOrganizationMapping = new HashMap<Integer, OrganizationMapping>();
		final List<RepositoryMapping> repositoriesToReturn = new ArrayList<RepositoryMapping>();
		for (RepositoryMapping repositoryMapping : repositoryMappings)
		{
			OrganizationMapping organizationMapping = idToOrganizationMapping
					.get(repositoryMapping.getOrganizationId());
			if (organizationMapping == null)
			{
				organizationMapping = getOrganizationMapping(repositoryMapping.getOrganizationId());
				if (organizationMapping == null)
				{
					// repository without organization ? invalid data
					//log.warn("Found repository without organization. Id = " + repositoryMapping.getID());
					continue;
				}
				// organizationMapping.getID() ==
				// repositoryMapping.getOrganizationId()
				idToOrganizationMapping.put(organizationMapping.getID(), organizationMapping);
				
			} 
			
			repositoriesToReturn.add(repositoryMapping);
		}

		final Collection<Repository> repositories = transformRepositories(idToOrganizationMapping, repositoriesToReturn);

		return new ArrayList<Repository>(repositories);

	}

	/**
	 * Transform repositories.
	 *
	 * @param idToOrganizationMapping the id to organization mapping
	 * @param repositoriesToReturn the repositories to return
	 * @return the collection< repository>
	 */
    @SuppressWarnings("unchecked")
	private Collection<Repository> transformRepositories(
			final Map<Integer, OrganizationMapping> idToOrganizationMapping,
			final List<RepositoryMapping> repositoriesToReturn)
	{
        return CollectionUtils.collect(repositoriesToReturn, new Transformer() {

            @Override
            public Object transform(Object input)
            {
                RepositoryMapping repositoryMapping = (RepositoryMapping) input;
                
                return RepositoryDaoImpl.this.transform(repositoryMapping,
                        idToOrganizationMapping.get(repositoryMapping.getOrganizationId()));
            }
        });
	}

	@Override
	public Repository get(final int repositoryId)
	{
		RepositoryMapping repositoryMapping = activeObjects
				.executeInTransaction(new TransactionCallback<RepositoryMapping>()
				{
					@Override
					public RepositoryMapping doInTransaction()
					{
						return activeObjects.get(RepositoryMapping.class, repositoryId);
					}
				});

		if (repositoryMapping == null) {
		
			log.warn("Repository with id {} was not found.", repositoryId);
			return null;

		} else {
			
			OrganizationMapping organizationMapping = getOrganizationMapping(repositoryMapping.getOrganizationId());
			return transform(repositoryMapping, organizationMapping);

		}
	}

	@Override
	public Repository save(final Repository repository)
	{
		final RepositoryMapping repositoryMapping = activeObjects
				.executeInTransaction(new TransactionCallback<RepositoryMapping>()
				{

					@Override
					public RepositoryMapping doInTransaction()
					{
						RepositoryMapping rm;
						if (repository.getId() == 0)
						{
                            // we need to remove null characters '\u0000' because PostgreSQL cannot store String values
                            // with such characters
							final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
							map.put(RepositoryMapping.ORGANIZATION_ID, repository.getOrganizationId());
							map.put(RepositoryMapping.SLUG, repository.getSlug());
							map.put(RepositoryMapping.NAME, repository.getName());
	                        map.put(RepositoryMapping.LAST_COMMIT_DATE, repository.getLastCommitDate());
							map.put(RepositoryMapping.LINKED, repository.isLinked());
							map.put(RepositoryMapping.DELETED, repository.isDeleted());
							map.put(RepositoryMapping.SMARTCOMMITS_ENABLED, repository.isSmartcommitsEnabled());
							map.put(RepositoryMapping.ACTIVITY_LAST_SYNC, repository.getActivityLastSync());

							rm = activeObjects.create(RepositoryMapping.class, map);
                            rm = activeObjects.find(RepositoryMapping.class, "ID = ?", rm.getID())[0];
						} else
						{
							rm = activeObjects.get(RepositoryMapping.class, repository.getId());

							rm.setSlug(repository.getSlug());
							rm.setName(repository.getName());
	                        rm.setLastCommitDate(repository.getLastCommitDate());
							rm.setLinked(repository.isLinked());
							rm.setDeleted(repository.isDeleted());
							rm.setSmartcommitsEnabled(repository.isSmartcommitsEnabled());
							rm.setActivityLastSync(repository.getActivityLastSync());

							rm.save();
						}
						return rm;
					}
				});

		return transform(repositoryMapping, getOrganizationMapping(repository.getOrganizationId()));

	}
	
    @Override
    public void setLastActivitySyncDate(final Integer id, final Date date)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            public Void doInTransaction()
            {
                RepositoryMapping repo = activeObjects.get(RepositoryMapping.class, id);
                repo.setActivityLastSync(date);
                repo.save();
                return null;
            }
        });
    }
	

    @Override
    public void remove(int repositoryId)
    {
        activeObjects.delete(activeObjects.get(RepositoryMapping.class, repositoryId));
    }

    private OrganizationMapping getOrganizationMapping(final int organizationId)
	{
		return activeObjects.executeInTransaction(new TransactionCallback<OrganizationMapping>()
		{
			@Override
			public OrganizationMapping doInTransaction()
			{
				return activeObjects.get(OrganizationMapping.class, organizationId);
			}
		});
	}

}
