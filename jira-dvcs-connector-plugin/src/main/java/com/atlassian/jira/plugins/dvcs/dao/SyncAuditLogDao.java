package com.atlassian.jira.plugins.dvcs.dao;

import java.util.Map;

import com.atlassian.jira.plugins.dvcs.activeobjects.v3.SyncAuditLogMapping;

public interface SyncAuditLogDao
{
    SyncAuditLogMapping newSyncAuditLog(Map<String, Object> data);

    SyncAuditLogMapping [] getAllForRepo(int repoId);

    int removeAllForRepo(int repoId);
}
