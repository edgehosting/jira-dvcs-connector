package com.atlassian.jira.plugins.dvcs.event;

import javax.annotation.Nonnull;

/**
 * No-op RepositorySync implementation.
 */
class NullRepositorySync implements RepositorySync
{
    @Nonnull
    @Override
    public RepositorySync storeEvents()
    {
        // do nothing
        return this;
    }

    @Override
    public void finishSync()
    {
        // do nothing
    }
}
