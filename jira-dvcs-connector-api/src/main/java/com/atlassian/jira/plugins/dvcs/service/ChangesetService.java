package com.atlassian.jira.plugins.dvcs.service;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;
import com.atlassian.jira.plugins.dvcs.model.Repository;

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

    Iterable<Changeset> getChangesetsFromDvcs(Repository repository);

    /**
     * returns all changesets for given issues
     * @param issueKeys set of issue keys
     * @return changesets
     */
    List<Changeset> getByIssueKey(Set<String> issueKeys);

    String getCommitUrl(Repository repository, Changeset changeset);

    Map<ChangesetFile, String> getFileCommitUrls(Repository repository, Changeset changeset);

    Iterable<Changeset> getLatestChangesets(int maxResults, GlobalFilter gf);

    void markSmartcommitAvailability(int id, boolean available);

    Set<String> findReferencedProjects(int repositoryId);

	Changeset getDetailChangesetFromDvcs(Repository repository, Changeset changeset);
}
