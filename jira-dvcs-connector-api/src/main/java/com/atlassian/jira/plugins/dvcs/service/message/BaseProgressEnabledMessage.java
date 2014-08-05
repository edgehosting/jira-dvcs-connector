package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;

public class BaseProgressEnabledMessage implements HasProgress
{
    Progress progress;

    int syncAuditId;

    boolean softSync;
    boolean webHookSync;

    Repository repository;

    protected BaseProgressEnabledMessage(Progress progress, int syncAuditId, boolean softSync, Repository repository, boolean webHookSync)
    {
        super();
        this.progress = progress;
        this.syncAuditId = syncAuditId;
        this.softSync = softSync;
        this.repository = repository;
        this.webHookSync = webHookSync;
    }

    public Progress getProgress()
    {
        return progress;
    }

    public int getSyncAuditId()
    {
        return syncAuditId;
    }

    public boolean isSoftSync()
    {
        return softSync;
    }

    public Repository getRepository()
    {
        return repository;
    }

    public boolean isWebHookSync()
    {
        return webHookSync;
    }
}
