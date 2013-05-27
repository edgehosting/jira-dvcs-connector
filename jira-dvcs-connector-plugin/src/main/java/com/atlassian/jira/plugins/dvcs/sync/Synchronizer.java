package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;

/**
 * Synchronization services
 */
public interface Synchronizer
{

    /**
     * Perform a sync on the specified dvcs repostiory from last already saved changeset
     * 
     * @param repository
     * @param synchronisationOperation
     */
    public void synchronize(Repository repository, SynchronisationOperation synchronisationOperation);

    /**
     * This tells that any runnnig or queued synchronisation for this repository should be canceled.
     * Used before deleting or unlinking repository.
     * 
     * @param repository
     */
    public void stopSynchronization(Repository repository);
    
    /**
     * Get the progress of a sync being executed for given repository
     * This will not return progress of sync triggered by postcommit hook. 
     *
     *
     * @param repositoryId@return
     */
    public Progress getProgress(int repositoryId);

    /**
     * Puts the progress for given repository
     * 
     * @param repository
     * @param progress
     */
    public void putProgress(Repository repository, Progress progress);
    
    /**
     * Removes the progress for given repository
     * 
     * @param repository
     */
    public void removeProgress(Repository repository);
}
