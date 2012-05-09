package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.model.Changeset;

public interface ChangesetDao
{
    void removeAllInRepository(int repositoryId);

    Changeset save(Changeset changeset);

    Changeset getByNode(int repositoryId, String changesetNode);
}
