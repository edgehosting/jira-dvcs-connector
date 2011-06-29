package com.atlassian.jira.plugins.bitbucket.connection;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketAuthentication;

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
    String getRepository(BitbucketAuthentication auth, String owner, String slug);

    /**
     * Load a single changeset
     * @param auth the authentication method
     * @param owner the owner of the repository
     * @param slug the slug of the repository
     * @param id the node id of the changeset to load
     * @return the json payload describing the changeset
     */
    String getChangeset(BitbucketAuthentication auth, String owner, String slug, String id);

    /**
     * Load a page of changesets for the given repository from the tip
     * @param auth the authentication method
     * @param owner the owner of the repository
     * @param slug the slug of the repository
     * @param limit the maximum number of revisions to include in the result
     * @return the json payload describing the changesets
     */
    String getChangesets(BitbucketAuthentication auth, String owner, String slug, int limit);

    /**
     * Load a page of changesets for the given repository from the given revision
     * @param auth the authentication method
     * @param owner the owner of the repository
     * @param slug the slug of the repository
     * @param start the revision to start the page or "tip" to start from the tip
     * @param limit the maximum number of revisions to include in the result
     * @return the json payload describing the changesets
     */
    String getChangesets(BitbucketAuthentication auth, String owner, String slug, int start, int limit);

    /**
     * Load a single user
     * @param username the username of the user to load
     * @return the json payload describing the user
     */
    String getUser(String username);
}
