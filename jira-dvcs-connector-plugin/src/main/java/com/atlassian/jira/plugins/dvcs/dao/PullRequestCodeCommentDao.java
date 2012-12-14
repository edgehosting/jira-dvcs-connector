package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketComment;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;

public interface PullRequestCodeCommentDao
{

    void save(BitbucketComment comment);
    
    void save(BitbucketPullRequest request);
}

