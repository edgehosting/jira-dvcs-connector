package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;

public interface SyncAuditLogDao
{
    SyncAuditLogMapping newSyncAuditLog(int repoId, String syncType);

    SyncAuditLogMapping finish(int syncId);

    SyncAuditLogMapping setException(int syncId, Throwable t);

    int removeAllForRepo(int repoId);

    boolean hasException(int syncId);

    SyncAuditLogMapping[] getAll();

    SyncAuditLogMapping[] getAllForRepo(int repoId);

    SyncAuditLogMapping getLastForRepo(int repoId);

    SyncAuditLogMapping getLastSuccessForRepo(int repoId);

    SyncAuditLogMapping getLastFailedForRepo(int repoId);

}
