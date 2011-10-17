package com.atlassian.jira.plugins.bitbucket.webwork;

import com.atlassian.jira.plugins.bitbucket.api.SourceControlRepository;

public interface BackwardCompability
{
	public SourceControlRepository getRepository(final String projectKey, final String repositoryUrl);
}
