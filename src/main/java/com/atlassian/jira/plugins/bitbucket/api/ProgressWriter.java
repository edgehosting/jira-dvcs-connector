package com.atlassian.jira.plugins.bitbucket.api;

/**
 * Interface used for 
 */
public interface ProgressWriter
{
	/**
	 * Call this method to update the current status of the progress.
	 * 
	 * @param changesetCount
	 * @param jiraCount
	 */
	public void inProgress(int changesetCount, int jiraCount);
}
