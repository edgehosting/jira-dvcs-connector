package com.atlassian.jira.plugins.bitbucket.mapper;

import java.util.List;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.IssueMapping;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v1.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;

/**
 * Maps bitbucket repositories and commits to jira projects and issues.
 */
public interface RepositoryPersister
{
    /**
     * Return a list of all repository uris for the given project
     * @param projectKey the jira project
     * @return a list of repositories
     */
	// TODO change to getRepositoryById(int id);
    ProjectMapping getRepository(String projectKey, RepositoryUri repositoryUri);

    /**
     * Return a list of all repository uris for the given project
     * @param projectKey the jira project
     * @return a list of repositories
     */
    List<ProjectMapping> getRepositories(String projectKey);

    /**
     * Map a repository to the specified jira project
     * @param projectKey the jira project
     * @param repositoryUri the uri of the repository to map to
     * @param username the username to use to connect to this bitbucket repository
     * @param password the password to use to connect to this bitbucket repository
     * @return 
     */
    ProjectMapping addRepository(String projectKey, RepositoryUri repositoryUri, String username, String password);

    /**
     * Remove the mapping of the bibucket repository from the specified jira project
     * @param projectKey the jira project
     * @param repositoryUri the uri of the repository to remove
     */
    void removeRepository(String projectKey, RepositoryUri repositoryUri);

    /**
     * Return a list of all commits mapped to the given issue from the given repository
     * @param repositoryManager 
     * @param issueId the jira issue id
     * @return a list of changesets
     */
    List<IssueMapping> getIssueMappings(String issueId);

    /**
     * Map a changeset to an issue id for the given repository
     * @param issueId the jira issue id
     * @param bitbucketChangeset the changeset to map to
     */
    void addChangeset(String issueId, Changeset bitbucketChangeset);

    /**
     * Remove the mapping of the bibucket changeset from the specified jira issue
     * @param issueId the jira issueid
     * @param bitbucketChangeset the changeset to remove from the mapping
     */
    void removeChangeset(String issueId, Changeset bitbucketChangeset);

}
