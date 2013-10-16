package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;

public interface SyncAuditLogDao
{
    SyncAuditLogMapping newSyncAuditLog(int repoId, String syncType);

    SyncAuditLogMapping[] getAllForRepo(int repoId);

    SyncAuditLogMapping getLastForRepo(int repoId);

    SyncAuditLogMapping finish(int syncId, Throwable exception);

    int removeAllForRepo(int repoId);
}
