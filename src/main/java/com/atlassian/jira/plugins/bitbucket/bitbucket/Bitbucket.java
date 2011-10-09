package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.common.Changeset;

/**
 * Starting point for remote API calls to the bitbucket remote API
 */
public interface Bitbucket
{
    /**
     * Retrieves information about a bitbucket user
     * @param username the user to load
     * @return the bitbucket user details
     */
    public BitbucketUser getUser(String username);

    /**
     * Retrieves information about a repository
     *
     * @param auth  the authentication rules for this request
     * @param owner the owner of the project
     * @param slug  the slug of the project
     * @return the project
     */
    public BitbucketRepository getRepository(Authentication auth, String owner, String slug);

    /**
     * Retrieves information about a changeset by changeset id
     *
     * @param auth  the authentication rules for this request
     * @param owner the owner of the project
     * @param slug  the slug of the project
     * @param id    the changeset id
     * @return the project
     */
    public Changeset getChangeset(Authentication auth, String owner, String slug, String id);

    /**
     * Retrieves all changesets for the specified repository
     *
     * @param auth  the authentication rules for this request
     * @param owner the owner of the project
     * @param slug  the slug of the project
     * @return the project
     */
    public Iterable<Changeset> getChangesets(Authentication auth, String owner, String slug);

}
