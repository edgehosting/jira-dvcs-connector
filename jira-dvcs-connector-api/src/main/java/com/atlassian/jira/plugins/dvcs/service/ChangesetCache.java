package com.atlassian.jira.plugins.dvcs.service;

public interface ChangesetCache
{

	/**
	 * @param repositoryId
	 * @param changesetNode
	 * @return true if the changeset is already in the DB
	 */
	public abstract boolean isCached(int repositoryId, String changesetNode);

	/**
	 * Indicates whether the cache is empty
	 *
	 * @param repositoryId
	 * @return true if no changest is present, false otherwise
	 */
    public abstract boolean isEmpty(int repositoryId);

}