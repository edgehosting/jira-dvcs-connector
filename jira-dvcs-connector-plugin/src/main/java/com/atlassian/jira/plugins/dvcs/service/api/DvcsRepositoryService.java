package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;
import java.util.Set;

/**
 * Gets the repositories for one or more organisations
 *
 * @since v1.4.3
 */
/*
This is a subset/partial service of {@link com.atlassian.jira.plugins.dvcs.service.RepositoryService}, that
only exposes basic and read-only information designed to be consumed externally
*/
public interface DvcsRepositoryService
{
    /**
     * Finds all repositories from all connected dvcs accounts
     *
     * @param includeDeleted whether to include deleted repositories
     * @return list of {@link Repository}
     */
    List<Repository> getRepositories(boolean includeDeleted);

    /**
     * Finds all repositories by an organization id
     *
     * @param organizationId the organization id to get
     * @param includeDeleted whether to include deleted repositories
     * @return list of {@link Repository}
     */
    List<Repository> getRepositories(int organizationId, boolean includeDeleted);

    /**
     * Find a repository by its ID
     *
     * @param repositoryId the repository ID to find
     * @return {@link Repository}
     */
    Repository getRepository(int repositoryId);

    /**
     * Retrieve the {@link DvcsUser} by a given author name within a {@link Repository} of the connected DVCS accounts
     *
     * @param repository the repository to find
     * @param author the author name to find
     * @param rawAuthor the author raw user name to find
     * @return {@link DvcsUser}
     */
    DvcsUser getDvcsUser(Repository repository, String author, String rawAuthor);

    List<Repository> getRepositories(String dvcsType, boolean includeDeleted);

    Set<String> getDvcsUserEmails(Repository repository, DvcsUser dvcsUser);
}
