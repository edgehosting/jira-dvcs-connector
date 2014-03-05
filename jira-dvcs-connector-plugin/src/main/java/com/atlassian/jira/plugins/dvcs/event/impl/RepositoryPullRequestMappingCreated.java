package com.atlassian.jira.plugins.dvcs.event.impl;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;

import javax.annotation.Nonnull;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Internal event indicating that a {@link RepositoryPullRequestMapping} was created.
 */
public class RepositoryPullRequestMappingCreated
{
    @Nonnull
    private final RepositoryPullRequestMapping pullRequestMapping;

    public RepositoryPullRequestMappingCreated(RepositoryPullRequestMapping pullRequestMapping)
    {
        this.pullRequestMapping = checkNotNull(pullRequestMapping, "pullRequestMapping");
    }

    @Nonnull
    public RepositoryPullRequestMapping getPullRequestMapping()
    {
        return pullRequestMapping;
    }
}
