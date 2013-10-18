package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.Progress;

public class BaseProgressEnabledMessage implements HasProgress
{

    private Progress progress;

    private int syncAuditId;

    protected BaseProgressEnabledMessage(Progress progress, int syncAuditId)
    {
        super();
        this.progress = progress;
        this.syncAuditId = syncAuditId;
    }

    public Progress getProgress()
    {
        return progress;
    }

    public int getSyncAuditId()
    {
        return syncAuditId;
    }



}
