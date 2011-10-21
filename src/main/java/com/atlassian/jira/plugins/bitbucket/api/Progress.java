package com.atlassian.jira.plugins.bitbucket.api;

public interface Progress
{
	boolean isFinished();

	int getJiraCount();
	int getChangesetCount();
	
	String getError();
}