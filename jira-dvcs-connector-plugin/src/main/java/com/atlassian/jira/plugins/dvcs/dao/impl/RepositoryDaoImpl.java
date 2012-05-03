package com.atlassian.jira.plugins.dvcs.dao.impl;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.OrganizationMapping;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.RepositoryMapping;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.sal.api.transaction.TransactionCallback;

import java.util.Collections;
import java.util.List;

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
    public List<Repository> getAllByOrganization(int organizationId)
    {
        // todo
        return Collections.<Repository>emptyList();
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
    public Repository save(Repository repository)
    {
        return null;
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
