package com.atlassian.jira.plugins.dvcs.sync;

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
     *
     * @return number of changesets which are not fully synchronised.
     */
    int getSynchroErrorCount();

	/**
	 * @return error messages
	 */
	String getError();
}