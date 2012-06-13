package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.GlobalFilter;

import java.util.List;

public interface ChangesetDao
{
    void removeAllInRepository(int repositoryId);

    Changeset save(Changeset changeset);

    Changeset getByNode(int repositoryId, String changesetNode);

    List<Changeset> getByIssueKey(String issueKey);

    List<Changeset> getLatestChangesets(int maxResults, GlobalFilter gf);
}
