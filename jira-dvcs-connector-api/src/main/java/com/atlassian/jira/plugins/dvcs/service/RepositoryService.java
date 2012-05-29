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
     * @param includeDeleted will contains also deleted
     * @return repositories
     */
    List<Repository> getAllByOrganization(int organizationId, boolean includeDeleted);
    
    /**
	 * Gets the all active (not deleted) repositories and their synchronization
	 * status.
	 * 
	 * @param organizationId
	 *            the organization id
	 * @return the all active repositories
	 */
    List<Repository> getAllRepositories();

    /**
     * check if there is at least one linked repository
     * @return true if there is at least one linked repository
     */
    boolean existsLinkedRepositories();

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
     * Synchronization of repository list in given organization
     *    Retrieves list of repositories for organization and adds/removes local repositories accordingly.
     *    If autolinking is set to to true new repositories will be linked and they will start synchronizing.
     *    
     * @param organization organization
     */
    void syncRepositoryList(Organization organization);

    /**
     * synchronization of changesets in given repository
     * @param repositoryId repositoryId
     * @param softSync
     */
    void sync(int repositoryId, boolean softSync);

    /**
     * synchronization of changesets in all repositories which are in given organization
     * @param organizationId organizationId
     */
    void syncAllInOrganization(int organizationId);

	/**
	 * Enables/links the repository to the jira projects. This will also
	 * (un)install postcommit hooks on repository and configure Links on
	 * bitbucket repositories
	 * 
	 * @param repoId
	 *            the repo id
	 * @param linked
	 *            the parse boolean
	 */
	void enableRepository(int repoId, boolean linked);

    /**
     * remove all repositories in organization.
     * @param organizationId organizationId
     */
    void removeAllInOrganization(int organizationId);

    /**
     * @param repository
     */
    void remove(Repository repository);
}
