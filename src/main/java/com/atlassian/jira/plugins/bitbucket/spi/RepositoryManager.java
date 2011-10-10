package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.List;
import java.util.concurrent.Callable;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.common.Changeset;
import com.atlassian.jira.plugins.bitbucket.mapper.OperationResult;
import com.atlassian.jira.plugins.bitbucket.mapper.Progress;
import com.atlassian.jira.plugins.bitbucket.mapper.SynchronizationKey;
import com.google.common.base.Function;

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

	public SourceControlUser getUser(String repositoryUrl, String username);

	public Callable<OperationResult> getSynchronisationOperation(SynchronizationKey key, Function<SynchronizationKey, Progress> progressProvider);

}