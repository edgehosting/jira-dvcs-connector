package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;



/**
 * Callback for synchronisation function
 */
public interface SynchronisationOperation
{
	void synchronise();

    public boolean isSoftSync();

    public DefaultProgress getProgress();
}
