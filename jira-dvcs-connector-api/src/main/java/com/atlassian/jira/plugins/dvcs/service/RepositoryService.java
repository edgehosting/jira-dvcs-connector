package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.SyncProgress;

import java.util.List;

/**
 * Returning type {@link Repository} is enriched with synchronization status by default.
 *
 *
 * @see SyncProgress
 *
 */
public interface RepositoryService
{

    /**
     * returns all repositories for given organization
     * @param organizationId organizationId
     * @param alsoDeleted will contains also deleted
     * @return repositories
     */
    List<Repository> getAllByOrganization(int organizationId, boolean alsoDeleted);
    
    /**
     * Gets the all active repositories with synchronization status.
     *
     * @param organizationId the organization id
     * @return the all active repositories
     */
    List<Repository> getAllActiveRepositories();

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

    /**
     * synchronization of repository list in given organization
     * @param organization organization
     */
    void syncRepositoryList(Organization organization);

    /**
     * synchronization of changesets in given repository
     * @param repositoryId repositoryId
     */
    void sync(int repositoryId, boolean softSync);

    /**
     * synchronization of changesets in all repositories which are in given organization
     * @param organizationId organizationId
     */
    void syncAllInOrganization(int organizationId);
}
