package com.atlassian.jira.plugins.dvcs.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryDao
{
    /**
     * returns all repositories for given organization
     * @param organizationId organizationId
     * @return repositories
     */
    List<Repository> getAllByOrganization(int organizationId, boolean alsoDeleted);

    /**
     * Gets all repositories across organizations.
     *
     * @param alsoDeleted the also deleted
     * @return the all
     */
    List<Repository> getAll(boolean alsoDeleted);

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
