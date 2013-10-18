package com.atlassian.jira.plugins.dvcs.service.message;

import com.atlassian.jira.plugins.dvcs.model.Progress;

public interface HasProgress
{
    Progress getProgress();

    int getSyncAuditId();
}
