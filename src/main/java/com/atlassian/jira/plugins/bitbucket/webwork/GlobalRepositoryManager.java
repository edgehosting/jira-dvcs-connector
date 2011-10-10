package com.atlassian.jira.plugins.bitbucket.webwork;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.mapper.OperationResult;
import com.atlassian.jira.plugins.bitbucket.mapper.Progress;
import com.atlassian.jira.plugins.bitbucket.mapper.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.google.common.base.Function;

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
		getManagerForUrl(changeset.getRepositoryUrl()).addChangeset(issueId, changeset);
	}

	public SourceControlUser getUser(String repositoryUrl, String username)
	{
		return getManagerForUrl(repositoryUrl).getUser(repositoryUrl, username);
	}

	public Callable<OperationResult> getSynchronisationOperation(SynchronizationKey key, Function<SynchronizationKey, Progress> progressProvider)
	{
		return getManagerForUrl(key.getRepositoryUri().getRepositoryUrl()).getSynchronisationOperation(key, progressProvider);
	}
}
