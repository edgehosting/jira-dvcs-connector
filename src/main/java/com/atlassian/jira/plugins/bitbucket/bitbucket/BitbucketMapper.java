package com.atlassian.jira.plugins.bitbucket.bitbucket;

import java.util.List;

/**
 * Maps bitbucket repositories and commits to jira projects and issues.
 */
public interface BitbucketMapper
{

    /**
     * Return a list of all repositories mapped to the given jira project.
     * @param projectKey the jira project
     * @return a list of repositories
     */
    List<BitbucketRepository> getRepositories(String projectKey);

    /**
     * Map a repository to the specified jira project
     * @param projectKey the jira project
     * @param repository the bitbucket repository
     */
    void addRepository(String projectKey, BitbucketRepository repository, String username, String password);

    /**
     * Remove the mapping of the bibucket repository from the specified jira project
     * @param projectKey the jira project
     * @param repository the bitbucket repository
     */
    void removeRepository(String projectKey, BitbucketRepository repository);

}
