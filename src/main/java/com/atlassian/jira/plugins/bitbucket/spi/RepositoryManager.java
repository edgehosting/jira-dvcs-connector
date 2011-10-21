package com.atlassian.jira.plugins.bitbucket.spi;

import java.util.List;

import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlUser;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;

public interface RepositoryManager
{
	
	/**
	 * Returns true if this RepositoryManager can manage repository with given url
	 * @param url
	 * @return
	 */
	public boolean canHandleUrl(String url);
	
	public SourceControlRepository addRepository(String projectKey, String repositoryUrl, String username, String password);
	
	public SourceControlRepository getRepository(int repositoryId);
	
	public List<SourceControlRepository> getRepositories(String projectKey);

	public List<Changeset> getChangesets(String issueKey);

	public void removeRepository(int id);

	public void addChangeset(SourceControlRepository sourceControlRepository, String issueId, Changeset changeset);

	public SourceControlUser getUser(String repositoryUrl, String username);

	public SynchronisationOperation getSynchronisationOperation(SynchronizationKey key, ProgressWriter progress);

	public List<Changeset> parsePayload(SourceControlRepository repository, String payload);

	public String getHtmlForChangeset(SourceControlRepository repository, Changeset changeset);

}