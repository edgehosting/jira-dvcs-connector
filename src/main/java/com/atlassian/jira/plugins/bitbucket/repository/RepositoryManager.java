package com.atlassian.jira.plugins.bitbucket.repository;

import java.util.List;

/**
 * Manages persistence of repository details.
 */
public interface RepositoryManager
{
    List<Repository> getRepositories(String projectKey);

    void addRepository(String projectKey, Repository repository);
}
