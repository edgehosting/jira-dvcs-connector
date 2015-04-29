package com.atlassian.jira.plugins.dvcs.spi.bitbucket.linker;

import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.Set;


public interface BitbucketLinker
{

	/**
	 * Configures bitbucket repository by adding links to all JIRA projects in this JIRA instance.
	 * 
	 * Note that current implementation makes one rest call for each project and the bitbucket is pretty
	 * slow in handling those call, taking almost one second per call. If there are 100 projects this method
	 * may take 100 seconds. 

	 * @param repository
	 */
	public void linkRepository(Repository repository, Set<String> projectsInChangesets);


	/**
	 * Removes all links that were previously configured by {@link #linkRepository(Repository)}
	 * 
	 * Note that current implementation makes one rest call for each project and the bitbucket is pretty
	 * slow in handling those call, taking almost one second per call. If there are 100 projects this method
	 * may take 100 seconds. 
	 * 
	 * @param repository
	 */
	public void unlinkRepository(Repository repository);
}
