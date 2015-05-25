package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;

public interface RepositoryDao
{
    /**
     * returns all repositories for given organization
     *
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

    boolean existsLinkedRepositories(boolean includeDeleted);

    /**
     * returns repository by ID or <code>null</code> if not found
     *
     * @param repositoryId repositoryId
     * @return repository
     */
    Repository get(int repositoryId);

    /**
     * save Repository to storage. If it's new object (without ID) after this operation it will have it assigned.
     *
     * @param repository Repository
     * @return Repository
     */
    Repository save(Repository repository);

    /**
     * @param repositoryId
     */
    void remove(int repositoryId);

    /**
     * Sets last pull request activity synchronization date
     *
     * @param repositoryId repository id
     * @param date last synchronization date
     */
    void setLastActivitySyncDate(Integer repositoryId, Date date);

    List<Repository> getAllByType(String dvcsType, boolean includeDeleted);

    List<String> getPreviouslyLinkedProjects(int repositoryId);

    void setPreviouslyLinkedProjects(int forRepositoryId, Set<String> projects);
}
