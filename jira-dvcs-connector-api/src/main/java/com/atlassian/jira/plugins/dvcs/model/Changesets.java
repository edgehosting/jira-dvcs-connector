package com.atlassian.jira.plugins.dvcs.model;

import com.google.common.base.Function;

import javax.annotation.Nullable;

/**
 * Utility functions for Changeset instances.
 */
public final class Changesets
{
    /**
     * Returns a Changeset's repository id.
     */
    public static final Function<Changeset, Integer> TO_REPOSITORY_ID = new Function<Changeset, Integer>()
    {
        @Override
        public Integer apply(@Nullable final Changeset changeset)
        {
            return changeset != null ? changeset.getRepositoryId() : null;
        }
    };
}
