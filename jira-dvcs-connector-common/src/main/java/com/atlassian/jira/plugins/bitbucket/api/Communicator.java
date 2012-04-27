package com.atlassian.jira.plugins.bitbucket.api;

import com.atlassian.jira.plugins.bitbucket.api.exception.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.rest.AccountInfo;
import com.atlassian.jira.plugins.bitbucket.api.rest.UrlInfo;

import java.util.Date;
import java.util.List;


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
     * @param repository
	 * @param lastCommitDate
     * @return
     */
    public Iterable<Changeset> getChangesets(SourceControlRepository repository, Date lastCommitDate);

    /**
     * @param repositoryUri
     * @param projectKey
     * @return info about the repository or null if repository is invalid
     */
    public UrlInfo getUrlInfo(RepositoryUri repositoryUri, String projectKey);



    /**
     * Retrieves pretty name of the repository from the server 
     * 
     * @param repositoryUri
     * @param adminUsername
     * @param adminPassword
     * @param accessToken
     * @return
     * @throws SourceControlException
     */
    public String getRepositoryName(RepositoryUri repositoryUri, String adminUsername, String adminPassword, String accessToken)
        throws SourceControlException;

    
    /**
     * Retrieves list of repositories for given organisation (user/account)
     * 
     * @param server
     * @param accountName
     * @param adminUsername
     * @param adminPassword
     * @param accessToken
     * @return
     */
    public List<String> getRepositoryNamesForAccount(String server, String accountName, String adminUsername, String adminPassword, String accessToken);

    /**
     * Returns information about the given account
     * 
     * @param server
     * @param accountName
     * @return
     */
    public AccountInfo getAccountInfo(String server, String accountName);
    
}
