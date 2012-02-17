package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;

import java.util.Date;

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

    public Changeset getChangeset(SourceControlRepository repository, Changeset changeset);


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

    /**
     *
     * @param repositoryManager
     * @param repository
	 * @param lastCommitDate
     * @return
     */
    public Iterable<Changeset> getChangesets(RepositoryManager repositoryManager, SourceControlRepository repository, Date lastCommitDate);

    /**
     * @param repositoryUri
     * @param projectKey
     * @return info about the repository or null if repository is invalid
     */
    public UrlInfo getUrlInfo(RepositoryUri repositoryUri, String projectKey);


    public String getRepositoryName(String repositoryType, String projectKey, RepositoryUri repositoryUri,
        String adminUsername, String adminPassword, String accessToken) throws SourceControlException;

}
