package com.atlassian.jira.plugins.bitbucket.api;

public interface ProgressWriter
{
	public void inProgress(int changesetCount, int jiraCount);
}
