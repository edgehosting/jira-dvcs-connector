package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;
import com.google.common.collect.Lists;
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
import com.atlassian.sal.api.transaction.TransactionCallback;

import javax.annotation.Resource;

public class RepositoryDaoImpl implements RepositoryDao
{

    private static final Logger log = LoggerFactory.getLogger(RepositoryDaoImpl.class);

    private final ActiveObjects activeObjects;

    @Resource
    private Synchronizer synchronizer;

    public RepositoryDaoImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    protected Repository transform(RepositoryMapping repositoryMapping)
    {
        if (repositoryMapping == null)
        {
            return null;
        }

        OrganizationMapping organizationMapping = activeObjects.get(OrganizationMapping.class, repositoryMapping.getOrganizationId());
        log.debug("Repository transformation: [{}] ", repositoryMapping);

        Repository repository = new Repository(repositoryMapping.getID(), repositoryMapping.getOrganizationId(), null,
                repositoryMapping.getSlug(), repositoryMapping.getName(), repositoryMapping.getLastCommitDate(),
                repositoryMapping.isLinked(), repositoryMapping.isDeleted(), null);
        repository.setSmartcommitsEnabled(repositoryMapping.isSmartcommitsEnabled());
        repository.setActivityLastSync(repositoryMapping.getActivityLastSync());

        Date lastDate = repositoryMapping.getLastCommitDate();

        if (lastDate == null || (repositoryMapping.getActivityLastSync() != null && repositoryMapping.getActivityLastSync().after(lastDate)))
        {
            lastDate = repositoryMapping.getActivityLastSync();
        }
        repository.setLastActivityDate(lastDate);
        repository.setLogo(repositoryMapping.getLogo());
        // set sync progress
        repository.setSync((DefaultProgress) synchronizer.getProgress(repository.getId()));
        repository.setFork(repositoryMapping.isFork());

        if (repository.isFork() && repositoryMapping.getForkOfSlug() != null)
        {
            Repository forkOfRepository = new Repository();
            forkOfRepository.setSlug(repositoryMapping.getForkOfSlug());
            forkOfRepository.setName(repositoryMapping.getForkOfName());
            forkOfRepository.setOwner(repositoryMapping.getForkOfOwner());
            if (organizationMapping != null)
            {
                forkOfRepository.setRepositoryUrl(createForkOfRepositoryUrl(repositoryMapping, organizationMapping));
            }
            repository.setForkOf(forkOfRepository);
        }

        if (organizationMapping != null)
        {
            Credential credential = new Credential(organizationMapping.getOauthKey(), organizationMapping.getOauthSecret(),
                    organizationMapping.getAccessToken(), organizationMapping.getAdminUsername(), organizationMapping.getAdminPassword());
            repository.setCredential(credential);
            repository.setDvcsType(organizationMapping.getDvcsType());
            repository.setOrgHostUrl(organizationMapping.getHostUrl());
            repository.setOrgName(organizationMapping.getName());
            repository.setRepositoryUrl(createRepositoryUrl(repositoryMapping, organizationMapping));
        } else
        {
            repository.setOrgHostUrl(null);
            repository.setOrgName(null);
            repository.setRepositoryUrl(null);
        }

        return repository;
    }

    private String createRepositoryUrl(RepositoryMapping repositoryMapping, OrganizationMapping organizationMapping)
    {
        return createRepositoryUrl(organizationMapping.getHostUrl(), organizationMapping.getName(), repositoryMapping.getSlug());
    }

    private String createForkOfRepositoryUrl(RepositoryMapping repositoryMapping, OrganizationMapping organizationMapping)
    {
        return createRepositoryUrl(organizationMapping.getHostUrl(), repositoryMapping.getForkOfOwner(), repositoryMapping.getForkOfSlug());
    }

    private String createRepositoryUrl(String hostUrl, String owner, String slug)
    {
        // normalize
        if (hostUrl != null && hostUrl.endsWith("/"))
        {
            hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
        }
        return hostUrl + "/" + owner + "/" + slug;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Repository> getAllByOrganization(final int organizationId, final boolean includeDeleted)
    {
        Query query = Query.select();
        if (includeDeleted)
        {
            query.where(RepositoryMapping.ORGANIZATION_ID + " = ? ", organizationId);
        } else
        {
            query.where(RepositoryMapping.ORGANIZATION_ID + " = ? AND " + RepositoryMapping.DELETED + " = ? ", organizationId,
                    Boolean.FALSE);
        }
        query.order(RepositoryMapping.NAME);

        final RepositoryMapping[] rms = activeObjects.find(RepositoryMapping.class, query);

        return (List<Repository>) CollectionUtils.collect(Arrays.asList(rms), new Transformer()
        {

            @Override
            public Object transform(Object input)
            {
                RepositoryMapping repositoryMapping = (RepositoryMapping) input;

                return RepositoryDaoImpl.this.transform(repositoryMapping);
            }
        });
    }

    @Override
    public List<Repository> getAll(final boolean includeDeleted)
    {
        Query select = Query.select();
        if (!includeDeleted)
        {
            select = select.where(RepositoryMapping.DELETED + " = ? ", Boolean.FALSE);
        }
        select.order(RepositoryMapping.NAME);

        final RepositoryMapping[] repos = activeObjects.find(RepositoryMapping.class, select);

        final Collection<Repository> repositories = transformRepositories(Arrays.asList(repos));

        return new ArrayList<Repository>(repositories);

    }

    @Override
    public List<Repository> getAllByType(final String dvcsType, final boolean includeDeleted)
    {
        Query select = Query.select()
                .alias(OrganizationMapping.class, "org")
                .alias(RepositoryMapping.class, "repo")
                .join(OrganizationMapping.class, "repo." + RepositoryMapping.ORGANIZATION_ID + " = org.ID");

        if (!includeDeleted)
        {
            select.where("org." + OrganizationMapping.DVCS_TYPE + " = ? AND repo." + RepositoryMapping.DELETED + " = ? ", dvcsType, Boolean.FALSE);
        } else
        {
            select.where("org." + OrganizationMapping.DVCS_TYPE + " = ?", dvcsType);
        }

        final RepositoryMapping[] repos = activeObjects.find(RepositoryMapping.class, select);

        final Collection<Repository> repositories = transformRepositories(Arrays.asList(repos));

        return new ArrayList<Repository>(repositories);

    }

    @Override
    public boolean existsLinkedRepositories(final boolean includeDeleted)
    {
        Query query = Query.select();
        if (includeDeleted)
        {
            query.where(RepositoryMapping.LINKED + " = ?", Boolean.TRUE);
        } else
        {
            query.where(RepositoryMapping.LINKED + " = ? AND " + RepositoryMapping.DELETED + " = ? ", Boolean.TRUE, Boolean.FALSE);
        }

        return activeObjects.count(RepositoryMapping.class, query) > 0;
    }

    /**
     * Transform repositories.
     *
     * @param repositoriesToReturn
     *            the repositories to return
     * @return the collection< repository>
     */
    @SuppressWarnings("unchecked")
    private Collection<Repository> transformRepositories(final List<RepositoryMapping> repositoriesToReturn)
    {
        return CollectionUtils.collect(repositoriesToReturn, new Transformer()
        {

            @Override
            public Object transform(Object input)
            {
                RepositoryMapping repositoryMapping = (RepositoryMapping) input;
                return RepositoryDaoImpl.this.transform(repositoryMapping);
            }
        });
    }

    @Override
    public Repository get(final int repositoryId)
    {
        RepositoryMapping repositoryMapping = activeObjects.executeInTransaction(new TransactionCallback<RepositoryMapping>()
        {
            @Override
            public RepositoryMapping doInTransaction()
            {
                return activeObjects.get(RepositoryMapping.class, repositoryId);
            }
        });

        if (repositoryMapping == null)
        {
            log.warn("Repository with id {} was not found.", repositoryId);
            return null;
        } else
        {
            return transform(repositoryMapping);

        }
    }

    @Override
    public Repository save(final Repository repository)
    {
        final RepositoryMapping repositoryMapping = activeObjects.executeInTransaction(new TransactionCallback<RepositoryMapping>()
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
                    map.put(RepositoryMapping.LOGO, repository.getLogo());
                    map.put(RepositoryMapping.IS_FORK, repository.isFork());
                    if (repository.getForkOf() != null)
                    {
                        map.put(RepositoryMapping.FORK_OF_NAME, repository.getForkOf().getName());
                        map.put(RepositoryMapping.FORK_OF_SLUG, repository.getForkOf().getSlug());
                        map.put(RepositoryMapping.FORK_OF_OWNER, repository.getForkOf().getOwner());
                    }
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
                    rm.setLogo(repository.getLogo());
                    rm.setFork(repository.isFork());
                    if (repository.getForkOf() != null)
                    {
                        rm.setForkOfName(repository.getForkOf().getName());
                        rm.setForkOfSlug(repository.getForkOf().getSlug());
                        rm.setForkOfOwner(repository.getForkOf().getOwner());
                    } else
                    {
                        rm.setForkOfName(null);
                        rm.setForkOfSlug(null);
                        rm.setForkOfOwner(null);
                    }
                    rm.save();
                }
                return rm;
            }
        });

        return transform(repositoryMapping);
    }

    @Override
    public void remove(int repositoryId)
    {
        activeObjects.delete(activeObjects.get(RepositoryMapping.class, repositoryId));
    }

    @Override
    public void setLastActivitySyncDate(final Integer repositoryId, final Date date)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            public Void doInTransaction()
            {
                RepositoryMapping repo = activeObjects.get(RepositoryMapping.class, repositoryId);
                repo.setActivityLastSync(date);
                repo.save();
                return null;
            }
        });
    }
}
