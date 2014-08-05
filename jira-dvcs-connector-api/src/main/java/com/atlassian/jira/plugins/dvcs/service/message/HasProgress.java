package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.Progress;

public interface HasProgress extends HasRepository
{
    Progress getProgress();

    int getSyncAuditId();

    boolean isSoftSync();

    boolean isWebHookSync();
}
