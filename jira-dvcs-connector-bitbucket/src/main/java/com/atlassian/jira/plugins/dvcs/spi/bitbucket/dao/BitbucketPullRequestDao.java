package com.atlassian.jira.plugins.dvcs.spi.bitbucket.dao;

import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.activeobjects.BitbucketPullRequestCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.activeobjects.BitbucketPullRequestContextMapping;

public interface BitbucketPullRequestDao
{
    void saveCommit(final int localId, final String node, final String nextNode, final int pullRequestId);

    BitbucketPullRequestCommitMapping getCommitForPullRequest(int pullRequestId, String node);
    void deleteCommit(BitbucketPullRequestCommitMapping commit);
    BitbucketPullRequestContextMapping getPulRequestContextForRemoteId(int repositoryId, long remoteId);
    void saveContext(Map<String,Object> context);

    BitbucketPullRequestContextMapping findPullRequestContextByRemoteId(int repositoryId, long remotePullRequestId);

    BitbucketPullRequestContextMapping[] findAllPullRequestContexts(int repositoryId);

    void clearContextForRepository(int repositoryId);
}
