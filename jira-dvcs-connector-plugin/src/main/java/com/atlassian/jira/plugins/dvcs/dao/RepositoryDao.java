package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;

public interface RepositoryDao
{
    /**
     * returns all repositories for given organization
     * @param organizationId organizationId
     * @return repositories
     */
    List<Repository> getAllByOrganization(int organizationId, boolean alsoDeleted);

    /**
     * returns repository by ID
     * @param repositoryId repositoryId
     * @return repository
     */
    Repository get(int repositoryId);

    /**
     * save Repository to storage. If it's new object (without ID) after this operation it will have it assigned.
     * @param repository Repository
     * @return Repository
     */
    Repository save(Repository repository);

}
