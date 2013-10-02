package com.atlassian.jira.plugins.dvcs.spi.bitbucket.dao.impl;

import java.util.HashMap;
import java.util.Map;

import net.java.ao.Query;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestUpdateActivityToCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.activeobjects.BitbucketPullRequestCommitMapping;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.activeobjects.BitbucketPullRequestContextMapping;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.dao.BitbucketPullRequestDao;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.sal.api.transaction.TransactionCallback;

public class BitbucketPullRequestDaoImpl implements BitbucketPullRequestDao
{
    private final ActiveObjects activeObjects;

    public BitbucketPullRequestDaoImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    @Override
    public BitbucketPullRequestCommitMapping saveCommit(final int localId, final String node, final String nextNode, final int pullRequestId)
    {
        return
        activeObjects.executeInTransaction(new TransactionCallback<BitbucketPullRequestCommitMapping>()
        {
            @Override
            public BitbucketPullRequestCommitMapping doInTransaction()
            {
                return activeObjects.create(BitbucketPullRequestCommitMapping.class, toDao(localId, node, nextNode, pullRequestId));
            }
        });
    }

    @Override
    public BitbucketPullRequestCommitMapping getCommitForPullRequest(int pullRequestId, String node)
    {
        Query query = Query.select().where(BitbucketPullRequestCommitMapping.PULL_REQUEST_ID + " = ? AND " + BitbucketPullRequestCommitMapping.NODE + " = ?", pullRequestId, node);
        BitbucketPullRequestCommitMapping[] found = activeObjects.find(BitbucketPullRequestCommitMapping.class, query);
        return found.length > 0 ? found[0] : null;
    }

    private Map<String, Object> toDao(int localId, String node, String nextNode, int pullRequestId)
    {
        Map<String, Object> map = new HashMap<String, Object>();

        map.put(BitbucketPullRequestCommitMapping.LOCAL_ID, localId);
        map.put(BitbucketPullRequestCommitMapping.NODE, node);
        map.put(BitbucketPullRequestCommitMapping.NEXT_NODE, nextNode);
        map.put(BitbucketPullRequestCommitMapping.PULL_REQUEST_ID, pullRequestId);

        return map;
    }

    @Override
    public void deleteCommit(BitbucketPullRequestCommitMapping commit)
    {
        activeObjects.delete(commit);
    }

    public void setPullRequestContext(final Map<String, Object> context)
    {
        activeObjects.executeInTransaction(new TransactionCallback<BitbucketPullRequestContextMapping>()
        {
            @Override
            public BitbucketPullRequestContextMapping doInTransaction()
            {
                return activeObjects.create(BitbucketPullRequestContextMapping.class, context);
            }
        });
    }

    @Override
    public BitbucketPullRequestContextMapping getPulRequestContextForRemoteId(int repositoryId, long remoteId)
    {
        Query query = Query.select()
                .where(BitbucketPullRequestContextMapping.REMOTE_PULL_REQUEST_ID +  " = ? AND "
                     + BitbucketPullRequestContextMapping.REPOSITORY_ID + " = ?", remoteId, repositoryId);

        BitbucketPullRequestContextMapping[] found = activeObjects.find(BitbucketPullRequestContextMapping.class, query);
        return found.length == 1 ? found[0] : null;
    }

    @Override
    public void clearContextForRepository(final int repositoryId)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                ActiveObjectsUtils.delete(
                        activeObjects,
                        BitbucketPullRequestContextMapping.class,
                        Query.select().from(BitbucketPullRequestContextMapping.class)
                                .where(BitbucketPullRequestContextMapping.REPOSITORY_ID + " = " + repositoryId));

                ActiveObjectsUtils.delete(
                        activeObjects,
                        RepositoryPullRequestUpdateActivityToCommitMapping.class,
                        Query.select()
                        		.alias(RepositoryPullRequestUpdateActivityToCommitMapping.class, "UPDATE_COMMIT")
                                .alias(BitbucketPullRequestCommitMapping.class, "COMMIT")
                                .alias(RepositoryPullRequestMapping.class, "PR")
                                .join(BitbucketPullRequestCommitMapping.class,
                                		"UPDATE_COMMIT.ID = COMMIT." + BitbucketPullRequestCommitMapping.LOCAL_ID)
                                .join(RepositoryPullRequestMapping.class,
                                        "COMMIT." + BitbucketPullRequestCommitMapping.PULL_REQUEST_ID + " = PR.ID")
                                .where("PR." + RepositoryPullRequestMapping.TO_REPO_ID + " = ? AND UPDATE_COMMIT." +
                                			RepositoryPullRequestUpdateActivityToCommitMapping.ACTIVITY + " is null", repositoryId));

                ActiveObjectsUtils.delete(
                        activeObjects,
                        BitbucketPullRequestCommitMapping.class,
                        Query.select()
                                .alias(BitbucketPullRequestCommitMapping.class, "COMMIT")
                                .alias(RepositoryPullRequestMapping.class, "PR")
                                .join(RepositoryPullRequestMapping.class,
                                        "COMMIT." + BitbucketPullRequestCommitMapping.PULL_REQUEST_ID + " = PR.ID")
                                .where("PR." + RepositoryPullRequestMapping.TO_REPO_ID + " = ?", repositoryId));
                return null;
            }
        });
    }

    @Override
    public void saveContext(final Map<String, Object> context)
    {
        activeObjects.executeInTransaction(new TransactionCallback<BitbucketPullRequestContextMapping>()
        {
            @Override
            public BitbucketPullRequestContextMapping doInTransaction()
            {
                return activeObjects.create(BitbucketPullRequestContextMapping.class, context);
            }

        });
    }

    @Override
    public BitbucketPullRequestContextMapping findPullRequestContextByRemoteId(int repositoryId, long remotePullRequestId)
    {
        Query query = Query
                .select()
                .from(BitbucketPullRequestContextMapping.class)
                .where(BitbucketPullRequestContextMapping.REMOTE_PULL_REQUEST_ID + " = ? AND " + BitbucketPullRequestContextMapping.REPOSITORY_ID + " = ?", remotePullRequestId,
                        repositoryId);

        BitbucketPullRequestContextMapping[] found = activeObjects.find(BitbucketPullRequestContextMapping.class, query);
        return found.length == 1 ? found[0] : null;
    }

    @Override
    public BitbucketPullRequestContextMapping[] findAllPullRequestContexts(int repositoryId)
    {
        Query query = Query
                .select()
                .from(BitbucketPullRequestContextMapping.class)
                .where(BitbucketPullRequestContextMapping.REPOSITORY_ID + " = ?", repositoryId);

        return activeObjects.find(BitbucketPullRequestContextMapping.class, query);
    }
}
