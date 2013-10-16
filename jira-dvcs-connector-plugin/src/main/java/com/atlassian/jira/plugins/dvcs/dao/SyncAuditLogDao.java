package com.atlassian.jira.plugins.dvcs.dao;

import java.util.Map;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;

public interface SyncAuditLogDao
{
    SyncAuditLogMapping newSyncAuditLog(Map<String, Object> data, int repoId);

    SyncAuditLogMapping [] getAllForRepo(int repoId);

    SyncAuditLogMapping getLastForRepo(int repoId);

    SyncAuditLogMapping finish(int syncId, Throwable exception);

    int removeAllForRepo(int repoId);
}
