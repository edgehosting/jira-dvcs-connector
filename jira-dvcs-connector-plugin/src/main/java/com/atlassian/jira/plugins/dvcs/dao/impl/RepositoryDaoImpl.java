package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RepositoryDaoImpl implements RepositoryDao
{

    private final ActiveObjects activeObjects;

    public RepositoryDaoImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    protected Repository transform(RepositoryMapping repositoryMapping, OrganizationMapping organizationMapping) {

        Credential credential = new Credential(organizationMapping.getAdminUsername(),
                organizationMapping.getAdminPassword(),
                organizationMapping.getAccessToken());


        Repository repository = new Repository(repositoryMapping.getID(),
                repositoryMapping.getOrganizationId(),
                organizationMapping.getDvcsType(),
                repositoryMapping.getSlug(),
                repositoryMapping.getName(),
                repositoryMapping.getLastCommitDate(),
                repositoryMapping.isLinked(),
                credential);

        return repository;
    }

    @Override
    public List<Repository> getAllByOrganization(final int organizationId)
    {
        List<RepositoryMapping> repositoryMappings = activeObjects.executeInTransaction(new TransactionCallback<List<RepositoryMapping>>()
        {
            @Override
            public List<RepositoryMapping> doInTransaction()
            {
                final RepositoryMapping[] rms = activeObjects.find(RepositoryMapping.class, RepositoryMapping.ORGANIZATION_ID + " = ?", organizationId);
                return Arrays.asList(rms);
            }
        });

        final OrganizationMapping organizationMapping = getOrganizationMapping(organizationId);

        final Collection<Repository> repositories = Collections2.transform(repositoryMappings, new Function<RepositoryMapping, Repository>()
        {
            @Override
            public Repository apply(RepositoryMapping repositoryMapping)
            {
                return transform(repositoryMapping, organizationMapping);
            }
        });

        return new ArrayList<Repository>(repositories);
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

        OrganizationMapping organizationMapping = getOrganizationMapping(repositoryMapping.getOrganizationId());
        return  transform(repositoryMapping, organizationMapping);
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
                    final Map<String, Object> map = new HashMap<String, Object>();
                    map.put(RepositoryMapping.ORGANIZATION_ID, repository.getOrganizationId());
                    map.put(RepositoryMapping.SLUG, repository.getSlug());
                    map.put(RepositoryMapping.NAME, repository.getName());
                    map.put(RepositoryMapping.LAST_COMMIT_DATE, repository.getLastCommitDate());
                    map.put(RepositoryMapping.LINKED, repository.isLinked());
                    map.put(RepositoryMapping.DELETED, repository.isDeleted());

                    rm = activeObjects.create(RepositoryMapping.class, map);
                } else {
                    rm = activeObjects.get(RepositoryMapping.class, repository.getId());

                    rm.setSlug(repository.getSlug());
                    rm.setName(repository.getName());
                    rm.setLastCommitDate(repository.getLastCommitDate());
                    rm.setLinked(repository.isLinked());
                    rm.setDeleted(repository.isDeleted());

                    rm.save();
                }

                return rm;
            }
        });

        return transform(repositoryMapping, getOrganizationMapping(repository.getOrganizationId()));

    }

    private OrganizationMapping getOrganizationMapping(final int organizationId) {
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
