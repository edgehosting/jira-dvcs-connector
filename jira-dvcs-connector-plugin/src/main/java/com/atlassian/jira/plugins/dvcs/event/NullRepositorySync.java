package com.atlassian.jira.plugins.dvcs.event;

/**
 * No-op RepositorySync implementation.
 */
class NullRepositorySync implements RepositorySync
{

    @Override
    public void finish()
    {
        // do nothing
    }
}
