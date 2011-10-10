package com.atlassian.jira.plugins.bitbucket.api;


public interface AuthenticationFactory
{
	public Authentication getAuthentication(SourceControlRepository repository);
}
