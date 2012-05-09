package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.Date;
import java.util.List;

public interface ChangesetService
{
    /**
     * returns all changesets for given issue
     * @param issueKey issueKey
     * @return changesets
     */
    List<Changeset> getAllByIssue(String issueKey);

    /**
     * save Changeset to storage. If it's new object (without ID) after this operation it will have it assigned.
     * @param changeset changeset
     * @return changeset
     */
    Changeset save(Changeset changeset);

    /**
     * remove all changesets in given repository
     * @param repositoryId repositoryId
     */
    void removeAll(int repositoryId);

    void removeAllInRepository(int repositoryId);

    Changeset getByNode(int repositoryId, String changesetNode);

    Iterable<Changeset> getChangesetsFromDvcs(Repository repository, Date lastCommitDate);

    Changeset getDetailChangesetFromDvcs(Organization organization, Repository repository, Changeset changeset);
}
