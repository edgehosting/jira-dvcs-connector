package com.atlassian.jira.plugins.dvcs.service;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.plugins.dvcs.model.Repository;

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

	@Override
	public List<Repository> getAllActiveRepositories()
	{
		// TODO Auto-generated method stub
		return new ArrayList<Repository>();
	}
    
}
