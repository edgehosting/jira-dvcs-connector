package com.atlassian.jira.plugins.bitbucket.spi.bitbucket.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.bitbucket.DefaultSynchronizer;
import com.atlassian.jira.plugins.bitbucket.api.Changeset;
import com.atlassian.jira.plugins.bitbucket.api.ProgressWriter;
import com.atlassian.jira.plugins.bitbucket.api.SynchronizationKey;
import com.atlassian.jira.plugins.bitbucket.spi.AbstractSynchronisationOperation;
import com.atlassian.jira.plugins.bitbucket.spi.RepositoryManager;
import com.atlassian.jira.plugins.bitbucket.spi.bitbucket.BitbucketCommunicator;

public class BitbucketSynchronisation extends AbstractSynchronisationOperation
{
    private final Logger logger = LoggerFactory.getLogger(DefaultSynchronizer.class);

	private BitbucketCommunicator bitbucketCommunicator;

    public BitbucketSynchronisation(SynchronizationKey key, RepositoryManager repositoryManager,
			BitbucketCommunicator bitbucketCommunicator, ProgressWriter progressProvider)
	{
    	super(key, repositoryManager, progressProvider);
		this.bitbucketCommunicator = bitbucketCommunicator;
	}

	public Iterable<Changeset> getChangsetsIterator()
	{
		logger.debug("synchronize [ {} ] with [ {} ]", key.getRepository().getProjectKey(), key.getRepository().getUrl());

        Iterable<Changeset> changesets = key.getChangesets() == null ? bitbucketCommunicator.getChangesets(key.getRepository()) : key.getChangesets();
		return changesets;
	}

}
