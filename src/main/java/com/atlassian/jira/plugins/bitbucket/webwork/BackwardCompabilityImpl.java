package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.bitbucket.activeobjects.v2.ProjectMapping;
import com.atlassian.jira.plugins.bitbucket.api.Encryptor;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlException;
import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.api.impl.DefaultSourceControlRepository;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.RepositoryUri;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;

/**
 * These methods used be in repository manager, but don't belong there anymore
 * 
 * @deprecated
 */
public class BackwardCompabilityImpl implements BackwardCompability
{

	private final ActiveObjects activeObjects;
	private final Encryptor encryptor;

	public BackwardCompabilityImpl(ActiveObjects activeObjects, Encryptor encryptor)
	{
		this.activeObjects = activeObjects;
		this.encryptor = encryptor;
	}

	/* Maps ProjectMapping to SourceControlRepository */
	private final Function<ProjectMapping, SourceControlRepository> TO_SOURCE_CONTROL_REPOSITORY = new Function<ProjectMapping, SourceControlRepository>()
	{
		public SourceControlRepository apply(ProjectMapping pm)
		{
			String decryptedPassword = encryptor.decrypt(pm.getPassword(), pm.getProjectKey(), pm.getRepositoryUrl());
			return new DefaultSourceControlRepository(pm.getID(), RepositoryUri.parse(pm.getRepositoryUrl())
					.getRepositoryUrl(), pm.getProjectKey(), pm.getUsername(), decryptedPassword);
		}
	};

	public SourceControlRepository getRepository(final String projectKey, final String repositoryUrl)
	{
		return activeObjects.executeInTransaction(new TransactionCallback<SourceControlRepository>()
		{
			public SourceControlRepository doInTransaction()
			{
				ProjectMapping[] mappings = activeObjects.find(ProjectMapping.class, "PROJECT_KEY = ? and REPOSITORY_URL = ?", projectKey, repositoryUrl);
				if (mappings.length == 0)
				{
					throw new SourceControlException("No repository with projectKey [" + projectKey + "] and url [" + repositoryUrl + "] found.");
				}
				return TO_SOURCE_CONTROL_REPOSITORY.apply(mappings[0]);
			}
		});
	}

}
