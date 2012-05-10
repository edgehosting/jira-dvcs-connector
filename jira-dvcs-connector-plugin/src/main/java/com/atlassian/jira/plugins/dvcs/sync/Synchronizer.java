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
     * @param softSync
     * @param repository
     */
    public void synchronize(Repository repository, boolean softSync);
    
    /**
     * Get the progress of a sync being executed for given repository
     * This will not return progress of sync triggered by postcommit hook. 
     *
     * @param repository
     * @return
     */
    public Progress getProgress(Repository repository);

}
