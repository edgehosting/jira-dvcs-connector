package com.atlassian.jira.plugins.bitbucket.api;

public interface Progress
{
	public void inProgress(String revision, int jiraCount);
	
	public String render();
}