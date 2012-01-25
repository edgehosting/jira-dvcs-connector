package com.atlassian.jira.plugins.bitbucket.api;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.streams.GlobalFilter;

import java.util.List;

/**
 * Maps bitbucket repositories and commits to jira projects and issues.
 */
public interface RepositoryPersister {
    /**
     * Return a list of all repository uris for the given project
     *
     * @param projectKey the jira project
     * @return a list of repositories
     */
    ProjectMapping getRepository(int id);

    /**
     * Return a list of all repository uris for the given project
     *
     * @param projectKey     the jira project
     * @param repositoryType
     * @return a list of repositories
     */
    List<ProjectMapping> getRepositories(String projectKey, String repositoryType);

    /**
     * Map a repository to the specified jira project
     * @param repositoryName the name of the repository
     * @param projectKey     the jira project
     * @param repositoryUrl  the uri of the repository to map to
     * @param username       the username to use to connect to this bitbucket repository
     * @param password       the password to use to connect to this bitbucket repository
     * @param adminUsername  the username of repository admin - used to create/delete postcommit hook
     * @param adminPassword  the password of repository admin - used to create/delete postcommit hook
     * @param repositoryType which type of repository is it (bitbucket, github, ... )
     * @param accessToken accessToken for github OAuth
     *
     * @return
     */
    ProjectMapping addRepository(String repositoryName, String projectKey, String repositoryUrl, String username, String password, String adminUsername, String adminPassword, String repositoryType, String accessToken);

    /**
     * Remove the mapping of the bibucket repository from the specified jira project
     *
     * @param projectKey    the jira project
     * @param repositoryUrl the uri of the repository to remove
     */
    void removeRepository(int id);

    /**
     * Return a list of all commits mapped to the given issue from the given repository
     *
     * @param repositoryManager
     * @param issueId           the jira issue id
     * @return a list of changesets
     */
    List<IssueMapping> getIssueMappings(String issueId, String repositoryType);

    /**
     * Map a changeset to an issue id for the given repository
     *
     * @param issueId            the jira issue id
     * @param bitbucketChangeset the changeset to map to
     */
    void addChangeset(final String issueId, Changeset changeset);

    /**
     * Find last changeset to the given count ordered by timestamp
     * @param count changesets count
     * @param repositoryType 
     * @return list of Changeset mappings
     */
    public List<IssueMapping> getLatestIssueMappings(int count, GlobalFilter gf, String repositoryType);

    /**
     * @param node
     * @return changeset by node
     */
    public IssueMapping getIssueMapping(String node);

}
