package com.atlassian.jira.plugins.dvcs.dao;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;

/**
 *
 */
public interface ChangesetDao
{
    /**
     * Removes all changesets from given repository
     * 
     * @param repositoryId
     */
    void removeAllInRepository(int repositoryId);

    /**
     * save Changeset to storage. If it's new object (without ID) after this operation it will have it assigned.
     * 
     * @param changeset
     * @return
     */
    Changeset save(Changeset changeset);

    /**
     * @param repositoryId
     * @param changesetNode
     * @return
     */
    Changeset getByNode(int repositoryId, String changesetNode);

    /**
     * Returns all changetsets related to given issueKey
     * 
     * @param issueKey
     * @return
     */
    List<Changeset> getByIssueKey(String issueKey);

    /**
     * Returns latest changesets. Used by activity stream.
     * 
     * @param maxResults
     * @param gf
     * @return
     */
    List<Changeset> getLatestChangesets(int maxResults, GlobalFilter gf);
    
    /**
     * Returns lists of latest commits that need to be processed by smartcommits logic.
     * 
     * @param closure
     */
    void forEachLatestChangesetsAvailableForSmartcommitDo(ForEachChangesetClosure closure);
    
    /**
     * 
     * @param id
     * @param available
     */
    void markSmartcommitAvailability(int id, boolean available);
    
    /**
     * Gets the project keys by repository.
     *
     * @param repositoryId the repository id
     * @return the project keys by repository
     */
    List<String> getOrderedProjectKeysByRepository(int repositoryId);
    
    /**
     *
     */
    public interface ForEachChangesetClosure
    {
        /**
         * @param changeset
         */
        void execute(Changeset changeset);
    }
}
