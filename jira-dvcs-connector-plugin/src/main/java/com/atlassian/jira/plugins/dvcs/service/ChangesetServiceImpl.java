package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Changeset;

import java.util.List;

public class ChangesetServiceImpl implements ChangesetService
{
    @Override
    public List<Changeset> getAllByIssue(String issueKey)
    {
        return null;
    }

    @Override
    public Changeset save(Changeset changeset)
    {
        return null;
    }

    @Override
    public void removeAll(int repositoryId)
    {
    }
}
