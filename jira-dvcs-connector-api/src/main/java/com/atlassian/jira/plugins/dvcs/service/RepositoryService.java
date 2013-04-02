package com.atlassian.jira.plugins.dvcs.service;

import java.util.EnumSet;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.RepositoryRegistration;
import com.atlassian.jira.plugins.dvcs.model.SyncProgress;
import com.atlassian.jira.plugins.dvcs.sync.SynchronizationFlag;

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
     * @return repositories
     */
    List<Repository> getAllByOrganization(int organizationId);
    
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
     * softsync is used by default
     * 
     * @param organization organization
     */
    void syncRepositoryList(Organization organization);

    void syncRepositoryList(Organization organization, boolean soft);

    /**
     * synchronization of changesets in given repository
     * @param repositoryId repositoryId
     * @param flags
     */
    void sync(int repositoryId, EnumSet<SynchronizationFlag> flags);

	/**
	 * Enables/links the repository to the jira projects. This will also
	 * (un)install postcommit hooks on repository and configure Links on
	 * bitbucket repositories
	 * 
	 * @param repoId
	 *            the repo id
	 * @param linked
	 *            the parse boolean
	 *            
	 * @returns {@link RepositoryRegistration}
	 */
	RepositoryRegistration enableRepository(int repoId, boolean linked);
	
	/**
	 * Enable repository smartcommits.
	 *
	 * @param repoId the repo id
	 * @param enabled the enabled
	 */
	void enableRepositorySmartcommits(int repoId, boolean enabled);

    /**
     * remove all repositories in organization.
     * @param organizationId organizationId
     */
    void removeAllInOrganization(int organizationId);

    /**
     * @param repository
     */
    void remove(Repository repository);
    
    /**
     * Turn On or off linkers.
     *
     * @param onOffBoolean the on off boolean
     */
    void onOffLinkers(boolean onOffBoolean);
    
    
    DvcsUser getUser(Repository repository, String author, String raw_author);
}
