package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;

public class RepositoryServiceImpl implements RepositoryService
{
    @Override
    public List<Repository> getAllByOrganization(int organizationId)
    {
        return null;
    }

    @Override
    public Repository get(int repositoryId)
    {
        return null;
    }

    @Override
    public Repository save(Repository repository)
    {
        return null;
    }

    @Override
    public void syncRepositoryList(int organizationId)
    {
    }

    @Override
    public void sync(int repositoryId)
    {
    }
}
