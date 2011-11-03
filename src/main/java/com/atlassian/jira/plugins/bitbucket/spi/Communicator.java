package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.List;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;

/**
 * Starting point for remote API calls to the bitbucket remote API
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
     * Retrieves changesets
     * 
     * @param repository
     * @param startNode
     * @param limit
     * @return
     */
    public List<Changeset> getChangesets(SourceControlRepository repository, String startNode, int limit);

    /**
     * @param repo
     * @param postCommitUrl 
     */
    public void setupPostcommitHook(SourceControlRepository repo, String postCommitUrl);

    /**
     * @param repo
     * @param postCommitUrl
     */
    public void removePostcommitHook(SourceControlRepository repo, String postCommitUrl);

}
