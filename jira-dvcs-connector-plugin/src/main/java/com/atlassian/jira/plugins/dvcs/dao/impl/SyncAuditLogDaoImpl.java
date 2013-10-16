package com.atlassian.jira.plugins.dvcs.dao.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import net.java.ao.Query;

import org.apache.commons.lang.exception.ExceptionUtils;

import com.atlassian.activeobjects.external.ActiveObjects;
import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;
import com.atlassian.jira.plugins.dvcs.dao.SyncAuditLogDao;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;

public class SyncAuditLogDaoImpl implements SyncAuditLogDao
{
    private ActiveObjects ao;

    public SyncAuditLogDaoImpl(ActiveObjects ao)
    {
        super();
        this.ao = ao;
    }

    @Override
    public SyncAuditLogMapping newSyncAuditLog(int repoId, String syncType)
    {
        Map<String, Object> data = new HashMap<String, Object>();
        data.put(SyncAuditLogMapping.REPO_ID, repoId);
        data.put(SyncAuditLogMapping.SYNC_TYPE, syncType);
        data.put(SyncAuditLogMapping.START_DATE, new Date());
        data.put(SyncAuditLogMapping.SYNC_STATUS, SyncAuditLogMapping.SYNC_STATUS_RUNNING);
        return ao.create(SyncAuditLogMapping.class, data);
    }

    @Override
    public SyncAuditLogMapping[] getAllForRepo(int repoId)
    {
        return ao.find(SyncAuditLogMapping.class, repoQuery(repoId).order(SyncAuditLogMapping.START_DATE + " DESC"));
    }

    @Override
    public SyncAuditLogMapping getLastForRepo(int repoId)
    {
        SyncAuditLogMapping[] found = ao.find(SyncAuditLogMapping.class, repoQuery(repoId).limit(1).order(SyncAuditLogMapping.START_DATE + " DESC"));
        return found.length == 1 ? found[0] : null;
    }

    @Override
    public SyncAuditLogMapping finish(int syncId, Throwable exception)
    {
        SyncAuditLogMapping mapping = find(syncId);
        if (mapping != null)
        {
            mapping.setEndDate(new Date());
            if (exception != null)
            {
                mapping.setExcTrace(ExceptionUtils.getStackTrace(exception));
                mapping.setSyncStatus(SyncAuditLogMapping.SYNC_STATUS_FAILED);
            } else
            {
                mapping.setSyncStatus(SyncAuditLogMapping.SYNC_STATUS_SUCCESS);
            }
            mapping.save();
        }
        return mapping;
    }

    @Override
    public int removeAllForRepo(int repoId)
    {
        return ActiveObjectsUtils.delete(ao, SyncAuditLogMapping.class, repoQuery(repoId));
    }

    private SyncAuditLogMapping find(int syncId)
    {
        return ao.get(SyncAuditLogMapping.class, syncId);
    }

    private Query repoQuery(int repoId)
    {
        return Query.select().from(SyncAuditLogMapping.class).where(SyncAuditLogMapping.REPO_ID + " = ?", repoId);
    }

}
