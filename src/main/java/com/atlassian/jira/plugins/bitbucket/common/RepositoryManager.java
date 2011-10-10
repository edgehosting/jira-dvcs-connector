package com.atlassian.jira.plugins.bitbucket.common;

import java.util.List;

public interface RepositoryManager
{
	/**
	 * Returns true if this RepositoryManager can manage repository with given url
	 * @param url
	 * @return
	 */
	public boolean canHandleUrl(String url);
	
	public SourceControlRepository addRepository(String projectKey, String repositoryUrl, String username, String password);
	
	public SourceControlRepository getRepository(String projectKey, String repositoryUrl);
	
	public List<SourceControlRepository> getRepositories(String projectKey);

	public List<Changeset> getChangesets(String issueKey);

	public void removeRepository(String projectKey, String url);

	public void addChangeset(String issueId, Changeset changeset);

}