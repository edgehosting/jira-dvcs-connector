package com.atlassian.jira.plugins.dvcs.dao;

import java.util.Date;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryDao
{
    /**
     * returns all repositories for given organization
     * @param organizationId organizationId
     * @return repositories
     */
    List<Repository> getAllByOrganization(int organizationId, boolean includeDeleted);

    /**
     * Gets all repositories across organizations.
     *
     * @param includeDeleted the also deleted
     * @return the all
     */
    List<Repository> getAll(boolean includeDeleted);

    /**
     * returns repository by ID or <code>null</code> if not found
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

    /**
     * @param repositoryId
     */
    void remove(int repositoryId);
    
    void setLastActivitySyncDate(Integer id, Date date);
}
