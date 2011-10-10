package com.atlassian.jira.plugins.bitbucket.webwork;

import java.util.ArrayList;
import java.util.List;

import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.common.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.common.SourceControlRepository;

/**
 * Aggregated Repository Manager that handles all Repository Managers based on the repository url
 */
public class GlobalRepositoryManager implements RepositoryManager
{
	public RepositoryManager[] repositoryManagers;

	public GlobalRepositoryManager(RepositoryManager... repositoryManagers)
	{
		this.repositoryManagers = repositoryManagers;
	}
	
	public boolean canHandleUrl(String url)
	{
    	for (RepositoryManager repositoryManager : repositoryManagers)
		{
    		if (repositoryManager.canHandleUrl(url))
    		{
    			return true;
    		}
		}		
		return false;
	}

	public SourceControlRepository addRepository(String projectKey, String url, String username, String password)
	{
		return getManagerForUrl(url).addRepository(projectKey, url, username, password);
	}


	public List<SourceControlRepository> getRepositories(String projectKey)
	{
		List<SourceControlRepository> allRepositories = new ArrayList<SourceControlRepository>();
    	for (RepositoryManager repositoryManager : repositoryManagers)
		{
			allRepositories.addAll(repositoryManager.getRepositories(projectKey));
		}		
    	return allRepositories;
	}

	public SourceControlRepository getRepository(String projectKey, String repositoryUrl)
	{
		return getManagerForUrl(repositoryUrl).getRepository(projectKey, repositoryUrl);
	}

	public List<Changeset> getChangesets(final String issueKey)
	{
		List<Changeset> allChangesets = new ArrayList<Changeset>();
    	for (RepositoryManager repositoryManager : repositoryManagers)
		{
			allChangesets.addAll(repositoryManager.getChangesets(issueKey));
		}		
    	return allChangesets;
	}

	public void removeRepository(String projectKey, String url)
	{
		getManagerForUrl(url).removeRepository(projectKey, url);
	};

	private RepositoryManager getManagerForUrl(String url)
	{
		for (RepositoryManager repositoryManager : repositoryManagers)
		{
			if (repositoryManager.canHandleUrl(url))
			{
				return repositoryManager;
			}
		}
		throw new IllegalArgumentException("No repository manager found for given url ["+url+"]");
	}

	public void addChangeset(String issueId, Changeset changeset)
	{
		RepositoryManager repositoryManager = getManagerForUrl(changeset.getRepositoryUrl());
		repositoryManager.addChangeset(issueId, changeset);
	}


}
