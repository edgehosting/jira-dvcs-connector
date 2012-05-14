package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.ChangesetFile;
import com.atlassian.jira.plugins.dvcs.model.DvcsUser;
import com.atlassian.jira.plugins.dvcs.model.Organization;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;

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

    void removeAllInRepository(int repositoryId);

    Changeset getByNode(int repositoryId, String changesetNode);

    Iterable<Changeset> getChangesetsFromDvcs(Repository repository, Date lastCommitDate);

    Changeset getDetailChangesetFromDvcs(Organization organization, Repository repository, Changeset changeset);

    List<Changeset> getByIssueKey(String issueKey);

    String getCommitUrl(Repository repository, Changeset changeset);

    Map<ChangesetFile, String> getFileCommitUrls(Repository repository, Changeset changeset);

    DvcsUser getUser(Repository repository, Changeset changeset);

    String getUserUrl(Repository repository, Changeset changeset);

}
