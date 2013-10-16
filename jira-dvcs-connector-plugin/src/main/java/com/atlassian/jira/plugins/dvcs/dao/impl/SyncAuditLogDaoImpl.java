package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import net.java.ao.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.atlassian.sal.api.transaction.TransactionCallback;

public class SyncAuditLogDaoImpl implements SyncAuditLogDao
{
    private final ActiveObjects ao;

    private static final Logger log = LoggerFactory.getLogger(SyncAuditLogDaoImpl.class);

    public SyncAuditLogDaoImpl(ActiveObjects ao)
    {
        super();
        this.ao = ao;
    }

    @Override
    public SyncAuditLogMapping newSyncAuditLog(final int repoId, final String syncType)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                Map<String, Object> data = new HashMap<String, Object>();
                data.put(SyncAuditLogMapping.REPO_ID, repoId);
                data.put(SyncAuditLogMapping.SYNC_TYPE, syncType);
                data.put(SyncAuditLogMapping.START_DATE, new Date());
                data.put(SyncAuditLogMapping.SYNC_STATUS, SyncAuditLogMapping.SYNC_STATUS_RUNNING);
                return ao.create(SyncAuditLogMapping.class, data);
            }
        });
    }

    @Override
    public SyncAuditLogMapping finish(final int syncId)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                SyncAuditLogMapping mapping = find(syncId);
                if (mapping != null)
                {
                    mapping.setEndDate(new Date());
                    if (StringUtils.isNotBlank(mapping.getExcTrace()))
                    {
                        mapping.setSyncStatus(SyncAuditLogMapping.SYNC_STATUS_FAILED);
                    } else
                    {
                        mapping.setSyncStatus(SyncAuditLogMapping.SYNC_STATUS_SUCCESS);
                    }
                    mapping.save();
                }
                return mapping;
            }
        });
    }

    @Override
    public int removeAllForRepo(final int repoId)
    {
        Integer ret = doTxQuietly(new Callable<Integer>(){
            @Override
            public Integer call() throws Exception
            {
                return ActiveObjectsUtils.delete(ao, SyncAuditLogMapping.class, repoQuery(repoId));
            }
        });
        return ret == null ? -1 : ret;
    }

    @Override
    public SyncAuditLogMapping setException(final int syncId, final Throwable t)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                SyncAuditLogMapping found = find(syncId);
                if (t != null)
                {
                    found.setExcTrace(ExceptionUtils.getStackTrace(t));
                    found.save();
                }
                return found;
            }
        });
    }

    @Override
    public SyncAuditLogMapping[] getAllForRepo(final int repoId)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping []>(){
            @Override
            public SyncAuditLogMapping [] call() throws Exception
            {
                return ao.find(SyncAuditLogMapping.class, repoQuery(repoId).order(SyncAuditLogMapping.START_DATE + " DESC"));
            }
        });
    }

    @Override
    public SyncAuditLogMapping[] getAll()
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping []>(){
            @Override
            public SyncAuditLogMapping [] call() throws Exception
            {
                return ao.find(SyncAuditLogMapping.class, Query.select().order(SyncAuditLogMapping.START_DATE + " DESC"));
            }
        });
    }

    @Override
    public SyncAuditLogMapping getLastForRepo(final int repoId)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                SyncAuditLogMapping[] found = ao.find(SyncAuditLogMapping.class,
                        repoQuery(repoId).limit(1).order(SyncAuditLogMapping.START_DATE + " DESC"));
                return found.length == 1 ? found[0] : null;
            }
        });
    }

    @Override
    public SyncAuditLogMapping getLastSuccessForRepo(final int repoId)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                Query query = statusQueryLimitOne(repoId, SyncAuditLogMapping.SYNC_STATUS_SUCCESS);
                SyncAuditLogMapping[] found = ao.find(SyncAuditLogMapping.class, query);
                return found.length == 1 ? found[0] : null;
            }
        });
    }

    @Override
    public SyncAuditLogMapping getLastFailedForRepo(final int repoId)
    {
        return doTxQuietly(new Callable<SyncAuditLogMapping>(){
            @Override
            public SyncAuditLogMapping call() throws Exception
            {
                Query query = statusQueryLimitOne(repoId, SyncAuditLogMapping.SYNC_STATUS_FAILED);
                SyncAuditLogMapping[] found = ao.find(SyncAuditLogMapping.class, query);
                return found.length == 1 ? found[0] : null;
            }
        });
    }

    @Override
    public boolean hasException(final int syncId)
    {
        Boolean ret = doTxQuietly(new Callable<Boolean>(){
            @Override
            public Boolean call() throws Exception
            {
                SyncAuditLogMapping found = find(syncId);
                return found != null && StringUtils.isNotBlank(found.getExcTrace());
            }
        });
        return ret == null ? false : ret;
    }

    private SyncAuditLogMapping find(int syncId)
    {
        return ao.get(SyncAuditLogMapping.class, syncId);
    }

    private Query repoQuery(int repoId)
    {
        return Query.select().from(SyncAuditLogMapping.class).where(SyncAuditLogMapping.REPO_ID + " = ?", repoId);
    }

    private Query statusQueryLimitOne(int repoId, String status)
    {
        return Query.select()
            .from(SyncAuditLogMapping.class)
            .where(SyncAuditLogMapping.REPO_ID + " = ? AND " + SyncAuditLogMapping.SYNC_STATUS + " = ?", repoId, status)
            .limit(1)
            .order(SyncAuditLogMapping.START_DATE + " DESC");
    }

    private <RET> RET doTxQuietly(final Callable<RET> callable) {
        return
        ao.executeInTransaction(new TransactionCallback<RET>()
        {
            @Override
            public RET doInTransaction()
            {
                try
                {
                    return callable.call();
                } catch (Throwable e)
                {
                    log.warn("Error during sync audit log.", e);
                    return null;
                }
            }
        });
    }
}
