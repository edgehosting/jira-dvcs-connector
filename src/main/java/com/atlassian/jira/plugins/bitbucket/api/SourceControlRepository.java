package com.atlassian.jira.plugins.bitbucket.api;

public interface SourceControlRepository
{
	int getId();
	
	String getUrl();

	String getUsername();

	String getPassword();

	String getProjectKey();
}
