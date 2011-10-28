package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;

/**
 * Starting point for remote API calls to the bitbucket remote API
 * TODO - this should be probably merged/united with {@link com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketConnection}
 */
public interface Communicator
{
    /**
     * Retrieves information about a bitbucket user
     * @param username the user to load
     * @return the bitbucket user details
     */
    public SourceControlUser getUser(SourceControlRepository repository, String username);

    /**
     * Retrieves information about a changeset by changeset id
     *
     * @param auth  the authentication rules for this request
     * @param owner the owner of the project
     * @param slug  the slug of the project
     * @param id    the changeset id
     * @return the project
     */
    public Changeset getChangeset(SourceControlRepository repository, String id);

    /**
     * Retrieves all changesets for the specified repository
     *
     * @param auth  the authentication rules for this request
     * @param owner the owner of the project
     * @param slug  the slug of the project
     * @return the project
     */
    public Iterable<Changeset> getChangesets(SourceControlRepository repository);

}
