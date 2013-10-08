package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivitySynchronizer;
import com.atlassian.jira.plugins.dvcs.dao.RepositoryDao;
import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;

public class BitbucketRepositoryActivitySynchronizer implements RepositoryActivitySynchronizer
{
    private final BitbucketClientBuilderFactory bitbucketClientBuilderFactory;
    private final RepositoryActivityDao dao;
    private RepositoryDao repositoryDao;

    public BitbucketRepositoryActivitySynchronizer(BitbucketClientBuilderFactory bitbucketClientBuilderFactory, RepositoryActivityDao dao,
                                                   RepositoryDao repositoryDao)
    {
        super();
        this.bitbucketClientBuilderFactory = bitbucketClientBuilderFactory;
        this.dao = dao;
        this.repositoryDao = repositoryDao;
    }

    @Override
    public void synchronize(Repository forRepository, Progress progress, boolean softSync)
    {

    }

}
