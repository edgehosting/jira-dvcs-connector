package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import net.java.ao.Entity;

import java.util.List;
import java.util.Set;

/**
 *
 */
public interface ChangesetDao
{
    /**
     * Removes all changesets from given repository
     */
    void removeAllInRepository(int repositoryId);

    /**
     * Calls {@link #createOrAssociate(com.atlassian.jira.plugins.dvcs.model.Changeset, java.util.Set)}.
     *
     * @deprecated Use {@link #createOrAssociate} instead.
     */
    @Deprecated
    Changeset create(Changeset changeset, Set<String> extractedIssues);

    /**
     * create Changeset and save to storage. If it's new object (without ID) after this operation it will have it
     * assigned. it's create alse all associations (repository- changeset, issues-changest)
     *
     * @return true if the changeset was created, false if it was updated
     * @since 2.1.17
     */
    boolean createOrAssociate(Changeset changeset, Set<String> extractedIssues);

    /**
     * update properties of changeset which is already saved in DB
     */
    Changeset update(Changeset changeset);

    /**
     * @param repositoryId
     * @param changesetNode
     * @return
     */
    Changeset getByNode(int repositoryId, String changesetNode);

    /**
     * Returns all changesets related to given issue keys
     */
    List<Changeset> getByIssueKey(Iterable<String> issueKeys, boolean newestFirst);

    /**
     * Returns all changesets related to given issue keys
     */
    List<Changeset> getByIssueKey(Iterable<String> issueKeys, String dvcsType, boolean newestFirst);

    List<Changeset> getByRepository(int repositoryId);

    /**
     * Returns latest changesets. Used by activity stream.
     */
    List<Changeset> getLatestChangesets(int maxResults, GlobalFilter gf);

    /**
     * Returns lists of latest commits that need to be processed by smartcommits logic.
     *
     * @param repositoryId id of the repository to select changesets from
     * @param columns the columns that are required in the closure
     * @param closure the code to be executed for each changeset selected
     */
    void forEachLatestChangesetsAvailableForSmartcommitDo(int repositoryId, String[] columns, ForEachChangesetClosure closure);

    int getNumberOfIssueKeysToChangeset();

    /**
     * Execute the supplied Function over every issue key mapping in the database
     *
     * @return true if all records are processed, false if the function chose to stop processing
     */
    boolean forEachIssueKeyMapping(final Organization organization, final Repository repository,
            final int pageSize, IssueToMappingFunction function);

    /**
     * @param id
     * @param available
     */
    void markSmartcommitAvailability(int id, boolean available);

    /**
     * From the changesets in database find all referenced project keys.
     *
     * @param repositoryId the repository id
     * @return the project keys by repository
     */
    Set<String> findReferencedProjects(int repositoryId);

    /**
     * Returns number of changesets synchronizes for the repository
     *
     * @return number of changesets
     */
    public int getChangesetCount(final int repositoryId);

    Set<String> findEmails(int repositoryId, String author);

    /**
     *
     */
    public interface ForEachChangesetClosure
    {
        /**
         * @param changeset
         */
        void execute(Entity changeset);
    }
}
