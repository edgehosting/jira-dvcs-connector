package com.atlassian.jira.plugins.bitbucket.spi.bitbucket;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;

/**
 * Interface to the bitbucket service
 */
public interface BitbucketConnection
{
    /**
     * Load a single repository
     * @param auth the authentication method
     * @param owner the owner of the repository
     * @param slug the slug of the repository
     * @return the json payload describing the repository
     */
    String getRepository(SourceControlRepository repository);

    /**
     * Load a single changeset
     * @param auth the authentication method
     * @param owner the owner of the repository
     * @param slug the slug of the repository
     * @param id the node id of the changeset to load
     * @return the json payload describing the changeset
     */
    String getChangeset(SourceControlRepository repository, String id);

    /**
     * Load a page of changesets for the given repository from the given revision
     * @param auth the authentication method
     * @param owner the owner of the repository
     * @param slug the slug of the repository
     * @param startNode the start node to start the page to be loaded
     * @param limit the maximum number of revisions to include in the result
     * @return the json payload describing the changesets
     */
    String getChangesets(SourceControlRepository repository, String startNode, int limit);

    /**
     * Load a single user
     * @param username the username of the user to load
     * @return the json payload describing the user
     */
    String getUser(SourceControlRepository repository, String username);

	/**
	 * Installs a postcommit service for this repository
	 * @param repo
	 * @param postCommitUrl 
	 */
	void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl);
}
