package com.atlassian.jira.plugins.dvcs.model;

/**
 * Information about the current synchronisation progress
 */
public interface Progress
{
    
    /**
     * Call this method to update the current status of the progress.
     * 
     * @param changesetCount
     * @param jiraCount
        * @param synchroErrorCount
     */
    void inProgress(int changesetCount, int jiraCount, int synchroErrorCount);

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

	/**
	 * Indication that the synchronisation should stop.
	 * Used when repository is unlinked or organisation and its repositories are deleted.
	 * 
	 * @param shouldStop
	 */
	void setShouldStop(boolean shouldStop);

	/**
	 * Indication that the synchronisation should stop 
	 * Used when repository is unlinked or organisation and its repositories are deleted.
	 * 
	 * @return
	 */
	boolean isShouldStop();


}