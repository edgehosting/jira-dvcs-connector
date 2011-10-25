package com.atlassian.jira.plugins.bitbucket.spi;


/**
 * Callback for synchronisation function
 */
public interface SynchronisationOperation
{
	void synchronise();
}
