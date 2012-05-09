package com.atlassian.jira.plugins.dvcs.model;

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
        * @param synchroErrorCount
	 */
	public void inProgress(int changesetCount, int jiraCount, int synchroErrorCount);
}
