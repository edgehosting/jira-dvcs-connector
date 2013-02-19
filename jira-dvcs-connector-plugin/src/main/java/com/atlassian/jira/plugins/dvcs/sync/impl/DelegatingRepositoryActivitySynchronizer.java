package com.atlassian.jira.plugins.dvcs.sync.impl;

import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.model.Repository;

public final class DelegatingRepositoryActivitySynchronizer implements RepositoryActivitySynchronizer
{

    private final RepositoryActivitySynchronizer bitbucketSynchronizer;
    private final RepositoryActivitySynchronizer githubSynchronizer;

    public DelegatingRepositoryActivitySynchronizer(
            @Qualifier("bitbucketRepositoryActivitySynchronizer")
            RepositoryActivitySynchronizer bitbucketSynchronizer,
            @Qualifier("githubRepositoryActivitySynchronizer")
            RepositoryActivitySynchronizer githubSynchronizer)
    {
        this.bitbucketSynchronizer = bitbucketSynchronizer;
        this.githubSynchronizer = githubSynchronizer;
    }

    @Override
    public void synchronize(Repository forRepository, boolean softSync)
    {
        if (isBitbucketRepo(forRepository))
        {
            bitbucketSynchronizer.synchronize(forRepository, softSync);
        } else
        {
            githubSynchronizer.synchronize(forRepository, softSync);
        }
    }

    private boolean isBitbucketRepo(Repository forRepository)
    {
        return "bitbucket".equals(forRepository.getDvcsType());
    }

}

