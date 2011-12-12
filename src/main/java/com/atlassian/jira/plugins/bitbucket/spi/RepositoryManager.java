package com.atlassian.jira.plugins.bitbucket.spi;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.streams.GlobalFilter;

import java.util.List;

public interface RepositoryManager
{

    /**
     * Mapps a repository to given project
     *
     * @param repositoryType
     * @param projectKey
     * @param repositoryUrl
     * @param username
     * @param password
     * @param adminUsername  - used when (un)installing postcommit hook
     * @param adminPassword  - used when (un)installing postcommit hook
     * @param accessToken    - token for authenticating if this repository is accessed using OAuth
     * @return
     */
    public SourceControlRepository addRepository(String repositoryType, String projectKey, String repositoryUrl, String username,
                                                 String password, String adminUsername, String adminPassword, String accessToken);

    /**
     * @param repositoryId
     * @return repository with given id
     *         throws IllegalArgumentException if repository with given id is not found
     */
    public SourceControlRepository getRepository(int repositoryId);

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
     * Parses the given json string into changesets
     *
     * @param repository
     * @param payload
     * @return
     */
    public List<Changeset> parsePayload(SourceControlRepository repository, String payload);

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
    public List<IssueMapping> getLastChangesets(final int count, GlobalFilter gf);

    /**
     * @param node
     * @return changeset by node
     */
    public Changeset getChangeset(String node);


    public UrlInfo getUrlInfo(String repositoryUrl);

    public Changeset reloadChangeset(IssueMapping issueMapping);

}