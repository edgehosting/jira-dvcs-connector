package com.atlassian.jira.plugins.dvcs.service;

public interface ChangesetCache
{

	/**
	 * @param repositoryId
	 * @param changesetNode
	 * @return true if the changeset is already in the DB
	 */
	public abstract boolean isCached(int repositoryId, String changesetNode);

}