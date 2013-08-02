package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import net.java.ao.Query;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.ActiveObjectsUtils;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.BranchHeadMapping;
import com.atlassian.jira.plugins.dvcs.dao.BranchDao;
import com.atlassian.jira.plugins.dvcs.model.BranchHead;
import com.atlassian.sal.api.transaction.TransactionCallback;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

public class BranchDaoImpl implements BranchDao
{
    private static final Logger log = LoggerFactory.getLogger(BranchDaoImpl.class);

    private final ActiveObjects activeObjects;

    public BranchDaoImpl(ActiveObjects activeObjects)
    {
        this.activeObjects = activeObjects;
    }

    @Override
    public List<BranchHead> getBranchHeads(int repositoryId)
    {
        BranchHeadMapping[] result = activeObjects.find(BranchHeadMapping.class, Query.select().where(BranchHeadMapping.REPOSITORY_ID + " = ?", repositoryId));

        return Lists.transform(Arrays.asList(result), new Function<BranchHeadMapping, BranchHead>()
        {
            @Override
            public BranchHead apply(BranchHeadMapping input)
            {
                return new BranchHead(input.getBranchName(), input.getHead());
            }
        });
    }

    @Override
    public void createBranchHead(final int repositoryId, final BranchHead branchHead)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                log.debug("adding branch head {} for repository with id = [ {} ]", new Object[]{branchHead, repositoryId});
                final Map<String, Object> map = new MapRemovingNullCharacterFromStringValues();
                map.put(BranchHeadMapping.REPOSITORY_ID, repositoryId);
                map.put(BranchHeadMapping.BRANCH_NAME, branchHead.getName());
                map.put(BranchHeadMapping.HEAD, branchHead.getHead());

                activeObjects.create(BranchHeadMapping.class, map);

                return null;
            }
        });
    }

    @Override
    public void removeBranchHead(final int repositoryId, final BranchHead branch)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                log.debug("deleting branch head {} for repository with id = [ {} ]", new Object[]{branch, repositoryId});
                Query query = Query.select().where(BranchHeadMapping.REPOSITORY_ID + " = ? AND " + BranchHeadMapping.BRANCH_NAME + " = ?", repositoryId, branch.getName());
                ActiveObjectsUtils.delete(activeObjects, BranchHeadMapping.class, query);
                return null;
            }
        });
    }

    @Override
    public void removeAllBranchHeadsInRepository(final int repositoryId)
    {
        activeObjects.executeInTransaction(new TransactionCallback<Void>()
        {
            @Override
            public Void doInTransaction()
            {
                Query query = Query.select().where(BranchHeadMapping.REPOSITORY_ID + " = ?", repositoryId);
                log.debug("deleting branches for repository with id = [ {} ]", repositoryId);
                ActiveObjectsUtils.delete(activeObjects, BranchHeadMapping.class, query);
                return null;
            }
        });
    }
}