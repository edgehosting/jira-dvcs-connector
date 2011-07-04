package com.atlassian.jira.plugins.bitbucket.mapper;

import com.atlassian.jira.plugins.bitbucket.bitbucket.BitbucketChangeset;
import com.atlassian.jira.plugins.bitbucket.bitbucket.RepositoryUri;

import java.util.List;


/**
 * Synchronization services
 */
public interface Synchronizer
{
    /**
     * Perform a full sync on the specified project with the specified bitbucket repostiory
     * @param projectKey the jira project key
     * @param repositoryUri the uri of the repository to synchronize
     */
    public void synchronize(String projectKey, RepositoryUri repositoryUri);

    /**
     * Perform a sync on the specified changesets with the specified project with the specified bitbucket repostiory
     * @param projectKey the jira project key
     * @param repositoryUri the uri of the repository to synchronize
     * @param changesets the changesets to synchronize
     */
    public void synchronize(String projectKey, RepositoryUri repositoryUri, List<BitbucketChangeset> changesets);

    /**
     * Get the progress of any sync being executed for the matching project and repository details
     * @param projectKey the jira project key
     * @param repositoryUri the uri of the repository
     * @return the progress of the synchronization
     */
    public SynchronizationProgress getProgress(String projectKey, RepositoryUri repositoryUri);

}
