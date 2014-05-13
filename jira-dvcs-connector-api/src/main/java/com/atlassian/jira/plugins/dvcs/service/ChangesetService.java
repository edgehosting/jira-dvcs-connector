package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ChangesetService
{
    /**
     * save Changeset to storage. If it's new object (without ID) after this operation it will have it assigned.
     *
     * @param changeset changeset
     * @param extractedIssues
     * @return changeset
     */
    Changeset create(Changeset changeset, Set<String> extractedIssues);

    /**
     * update properties of changeset which is already saved in DB
     *
     * @param changeset
     * @return
     */
    Changeset update(Changeset changeset);

    void removeAllInRepository(int repositoryId);

    /**
     * returns all changesets for given issues
     * @param issueKeys set of issue keys
     * @param newestFirst if true the newest changesets will be the first
     * @return changesets
     */
    List<Changeset> getByIssueKey(Iterable<String> issueKeys, boolean newestFirst);

    List<Changeset> getByIssueKey(Iterable<String> issueKeys, String dvcsType, boolean newestFirst);

    String getCommitUrl(Repository repository, Changeset changeset);

    Map<ChangesetFile, String> getFileCommitUrls(Repository repository, Changeset changeset);

    List<Changeset> getChangesets(Repository repository);

    Iterable<Changeset> getLatestChangesets(int maxResults, GlobalFilter gf);

    void markSmartcommitAvailability(int id, boolean available);

    Set<String> findReferencedProjects(int repositoryId);
    
    Changeset getByNode(int repositoryId, String changesetNode);

    /**
     * Ensures that the passed in Changeset instances have file details, fetching them from the remote system if
     * necessary. Note that this is a potentially expensive operation which should not be performed systematically but
     * should instead be triggered by a user action.
     *
     * @param changesets the Changesets
     * @return a read-only list of Changesets with file details
     * @since 2.0.0
     */
    List<Changeset> getChangesetsWithFileDetails(List<Changeset> changesets);

    Set<String> findEmails(int repositoryId, String author);
}
