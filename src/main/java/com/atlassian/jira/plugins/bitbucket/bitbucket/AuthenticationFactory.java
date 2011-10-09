package com.atlassian.jira.plugins.bitbucket.bitbucket;

import com.atlassian.jira.plugins.bitbucket.common.SourceControlRepository;

public interface AuthenticationFactory
{
	public Authentication getAuthentication(SourceControlRepository repository);
}
