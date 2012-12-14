package com.atlassian.jira.plugins.dvcs.sync.impl;

import com.atlassian.jira.plugins.dvcs.dao.PullRequestCodeCommentDao;
import com.atlassian.jira.plugins.dvcs.model.DefaultProgress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientRemoteFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;
import com.atlassian.jira.plugins.dvcs.sync.SynchronisationOperation;

public class PullRequestSynchronizatonOperation implements SynchronisationOperation
{

    private final Repository forRepository;
    private final BitbucketClientRemoteFactory clientRemoteFactory;
    private final PullRequestCodeCommentDao pullRequestCommentsDao;

    public PullRequestSynchronizatonOperation(Repository repository,
                                            BitbucketClientRemoteFactory clientRemoteFactory,
                                            PullRequestCodeCommentDao pullRequestCommentsDao)
    {
        this.forRepository = repository;
        this.clientRemoteFactory = clientRemoteFactory;
        this.pullRequestCommentsDao = pullRequestCommentsDao;
    }
    
    @Override
    public void synchronise()
    {
        BitbucketRemoteClient remoteClient = clientRemoteFactory.getForRepository(forRepository);
        PullRequestRemoteRestpoint remoteRestpoint = remoteClient.getPullRequestAndCommentsRemoteRestpoint();
     
    }

    @Override
    public boolean isSoftSync()
    {
        return false;
    }

    @Override
    public DefaultProgress getProgress()
    {
        return null;
    }

}

