package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import java.util.Iterator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.DefaultSynchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.AbstractSynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.spi.Communicator;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;

public class BitbucketSynchronisation extends AbstractSynchronisationOperation
{
    private final Logger logger = LoggerFactory.getLogger(DefaultSynchronizer.class);
    private final Communicator bitbucketCommunicator;

    public BitbucketSynchronisation(SynchronizationKey key, RepositoryManager repositoryManager,
            Communicator bitbucketCommunicator, ProgressWriter progressProvider)
    {
        super(key, repositoryManager, progressProvider);
        this.bitbucketCommunicator = bitbucketCommunicator;
    }

    @Override
    public Iterable<Changeset> getChangsetsIterator()
    {
        logger.debug("synchronize [ {} ] with [ {} ]", key.getRepository().getProjectKey(), key.getRepository().getUrl());

        if (key.getChangesets() != null)
        {
            return key.getChangesets();
        }

        return new Iterable<Changeset>()
        {
            public Iterator<Changeset> iterator()
            {
                return new BitbucketChangesetIterator(bitbucketCommunicator, key.getRepository());
            }
        };
    }
}
