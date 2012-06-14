package com.atlassian.jira.plugins.bitbucket.api;

import com.atlassian.jira.plugins.bitbucket.api.impl.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.api.rest.UrlInfo;
import com.atlassian.jira.plugins.bitbucket.api.streams.GlobalFilter;

import java.util.Date;
import java.util.List;
import java.util.Set;

public interface RepositoryManager
{

    /**
     * @param repositoryId
     * @return repository with given id
     *         throws IllegalArgumentException if repository with given id is not found
     */
    public SourceControlRepository getRepository(int repositoryId);

    /**
     * Mapps a repository to given project
     *
     * @param repositoryType
     * @param projectKey
     * @param repositoryUrl
     * @param adminUsername  - used when (un)installing postcommit hook
     * @param adminPassword  - used when (un)installing postcommit hook
     * @param accessToken    - token for authenticating if this repository is accessed using OAuth
     * @return
     */
    public SourceControlRepository addRepository(String repositoryType, String projectKey, String repositoryUrl,
                                                 String adminUsername, String adminPassword, String accessToken);

    /**
     * @param projectKey
     * @return list of repositories linked with given project
     */
    public List<SourceControlRepository> getRepositories(String projectKey);

    /**
     * @param issueKey
     * @return list of changesets linked to the given issue
     */
    public List<Changeset> getChangesets(String issueKey);


    public Changeset getDetailChangeset(SourceControlRepository repository, Changeset changeset);

    /**
     * Removes the repository with given id and all the issue mappings for this repository
     *
     * @param id
     */
    public void removeRepository(int id);

    /**
     * Links changeset with given issue
     *
     * @param sourceControlRepository
     * @param issueId
     * @param changeset
     * @return true if changeset is fully loaded from repo (with statistics) and saved, otherwise false.
     */
    public void addChangeset(SourceControlRepository sourceControlRepository, String issueId, Changeset changeset);

    /**
     * @param repositoryUrl
     * @param username
     * @return the details about the user
     */
    public SourceControlUser getUser(SourceControlRepository repository, String username);

    /**
     * Returns callback function that is used for synchronisation of the repository.
     *
     * @param key
     * @param progress
     * @return
     */
    public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progress);

    /**
     * @param repository
     * @param changeset
     * @return the html used for rendering this changeset on the issue.
     */
    public String getHtmlForChangeset(SourceControlRepository repository, Changeset changeset);

    /**
     * Installs a postcommit service for this repository
     *
     * @param repo
     */
    public void setupPostcommitHook(SourceControlRepository repo);

    /**
     * Removes a postcommit service for this repository
     *
     * @param repo
     */
    public void removePostcommitHook(SourceControlRepository repo);

    /**
     * @return the identifier of repository type
     */
    public String getRepositoryType();

    /**
     * Find last changeset to the given count ordered by timestamp
     *
     * @param count         changesets count
     * @param inProjects
     * @param notInProjects @return list of Changeset mappings
     */
    public Set<Changeset> getLatestChangesets(final int count, GlobalFilter gf);

    /**
     * @param repositoryUrl
     * @param projectKey
     * @return
     */
    public UrlInfo getUrlInfo(String repositoryUrl, String projectKey);

    /**
     * Reloads the changeset from the repository.
     * In previous versions of the plugin we stored  little information about changesets locally (only changset id).
     * Now we keep more columns (date, message, author, etc) but instead of resyncing all repositories again we use
     * lazy loading to reload old changesets only when required.
     *
     * @param repositoryId
     * @param node
     * @param issueId
     * @param branch
     * @return
     */
    public Changeset reloadChangeset(int repositoryId, String node, String issueId, String branch);

    public Date getLastCommitDate(SourceControlRepository repo);

    public void setLastCommitDate(SourceControlRepository repo, Date date);

    public void removeAllChangesets(int repositoryId);

}