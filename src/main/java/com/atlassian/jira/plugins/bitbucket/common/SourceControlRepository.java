package com.atlassian.jira.plugins.bitbucket.common;

public interface SourceControlRepository
{
	int getId();
	
	String getUrl();

	String getUsername();

	String getPassword();

	String getProjectKey();
}
