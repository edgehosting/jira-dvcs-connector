package com.atlassian.jira.plugins.bitbucket;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.StringUtils;

import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.RepositoryPersister;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.SynchronisationOperation;

/**
 * Aggregated Repository Manager that handles all Repository Managers based on the repository url
 */
public class GlobalRepositoryManager implements RepositoryManager
{
	public RepositoryManager[] repositoryManagers;
	private final RepositoryPersister repositoryPersister;

	public GlobalRepositoryManager(RepositoryPersister repositoryPersister, RepositoryManager... repositoryManagers)
	{
		this.repositoryPersister = repositoryPersister;
		this.repositoryManagers = repositoryManagers;
	}
	
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
	
	private RepositoryManager getManagerByRepoId(int id)
	{
		ProjectMapping repository = repositoryPersister.getRepository(id);
		if (repository == null)
			throw new IllegalArgumentException("No repository with id = '"+id+"' found");
		String repositoryUrl = repository.getRepositoryUrl();
		if (StringUtils.isEmpty(repositoryUrl))
			throw new IllegalArgumentException("The url for repository ["+id+"] is empty");
		return getManagerForUrl(repositoryUrl);
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

	
	public SourceControlRepository getRepository(int id)
	{
		return getManagerByRepoId(id).getRepository(id);
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

	public void removeRepository(int id)
	{
		getManagerByRepoId(id).removeRepository(id);
	};


	public void addChangeset(SourceControlRepository repository, String issueId, Changeset changeset)
	{
		getManagerForUrl(repository.getUrl()).addChangeset(repository, issueId, changeset);
	}

	public SourceControlUser getUser(String repositoryUrl, String username)
	{
		return getManagerForUrl(repositoryUrl).getUser(repositoryUrl, username);
	}

	public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progressProvider)
	{
		String repositoryUrl = key.getRepository().getUrl();
		return getManagerForUrl(repositoryUrl).getSynchronisationOperation(key, progressProvider);
	}

	public List<Changeset> parsePayload(SourceControlRepository repository, String payload)
	{
		return getManagerForUrl(repository.getUrl()).parsePayload(repository, payload);
	}

	public String getHtmlForChangeset(SourceControlRepository repository, Changeset changeset)
	{
		return getManagerForUrl(repository.getUrl()).getHtmlForChangeset(repository, changeset);
	}
}
