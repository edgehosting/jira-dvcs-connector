package com.atlassian.jira.plugins.bitbucket;

public interface Progress
{
	public void inProgress(String revision, int jiraCount);
	
}