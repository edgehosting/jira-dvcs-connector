package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;

public interface SyncAuditLogDao
{
    SyncAuditLogMapping newSyncAuditLog(int repoId, String syncType);

    SyncAuditLogMapping finish(int syncId);

    SyncAuditLogMapping setException(int syncId, Throwable t, boolean overwriteOld);

    int removeAllForRepo(int repoId);

    boolean hasException(int syncId);

    SyncAuditLogMapping[] getAll(Integer page);

    SyncAuditLogMapping[] getAllForRepo(int repoId, Integer page);

    SyncAuditLogMapping getLastForRepo(int repoId);

    SyncAuditLogMapping getLastSuccessForRepo(int repoId);

    SyncAuditLogMapping getLastFailedForRepo(int repoId);

}
