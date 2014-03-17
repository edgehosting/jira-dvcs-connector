package com.atlassian.jira.plugins.dvcs.dao;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;

import java.util.Date;

public interface SyncAuditLogDao
{
    SyncAuditLogMapping newSyncAuditLog(int repoId, String syncType, Date startDate);

    SyncAuditLogMapping finish(final int syncId, final Date firstRequestDate, final int numRequests, final int flightTimeMs, final Date finishDate);

    SyncAuditLogMapping setException(int syncId, Throwable t, boolean overwriteOld);

    int removeAllForRepo(int repoId);

    boolean hasException(int syncId);

    SyncAuditLogMapping[] getAll(Integer page);

    SyncAuditLogMapping[] getAllForRepo(int repoId, Integer page);

    SyncAuditLogMapping getLastForRepo(int repoId);

    SyncAuditLogMapping getLastSuccessForRepo(int repoId);

    SyncAuditLogMapping getLastFailedForRepo(int repoId);

    SyncAuditLogMapping pause(int syncId);

    SyncAuditLogMapping resume(int syncId);
}
