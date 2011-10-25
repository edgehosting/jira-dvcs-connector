package com.atlassian.jira.plugins.bitbucket;

import java.util.List;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.Progress;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;


/**
 * Synchronization services
 */
public interface Synchronizer
{
    /**
     * Perform a full sync on the specified project with the specified bitbucket repostiory
     * @param projectKey the jira project key
     * @param repositoryUrl the uri of the repository to synchronize
     */
    public void synchronize(SourceControlRepository repository);

    /**
     * Perform a sync on the specified changesets with the specified project with the specified bitbucket repostiory
     * @param projectKey the jira project key
     * @param repositoryUrl the uri of the repository to synchronize
     * @param changesets the changesets to synchronize
     */
    public void synchronize(SourceControlRepository repository, List<Changeset> changesets);
    
    /**
     * Get the progress of a sync being executed for given repository
     * This will not return progress of sync triggered by postcommit hook. 
     * @param repository
     * @return
     */
    public Progress getProgress(SourceControlRepository repository);

}
