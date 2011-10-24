package com.atlassian.jira.plugins.bitbucket.api;

/**
 * Information about the current synchronisation progress
 */
public interface Progress
{
	/**
	 * @return true if the progress is Finished
	 */
	boolean isFinished();

	/**
	 * @return number of JIRA issues found in commit messages
	 */
	int getJiraCount();
	
	/**
	 * @return number of changesets synchronised
	 */
	int getChangesetCount();
	
	/**
	 * @return error messages
	 */
	String getError();
}