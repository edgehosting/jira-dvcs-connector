package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.dao.ChangesetDao;

/**
 * Used by github synchronisation to figure out which changesets have been already synchronized
 *
 */
public class ChangesetCacheImpl implements ChangesetCache
{
	private final ChangesetDao changesetDao;

	public ChangesetCacheImpl(ChangesetDao changesetDao)
    {
		this.changesetDao = changesetDao;
    }

	/* (non-Javadoc)
     * @see com.atlassian.jira.plugins.dvcs.service.ChangesetCache#isCached(int, java.lang.String)
     */
	@Override
    public boolean isCached(int repositoryId, String changesetNode)
    {
        return changesetDao.getByNode(repositoryId, changesetNode) !=null;
    }

}
