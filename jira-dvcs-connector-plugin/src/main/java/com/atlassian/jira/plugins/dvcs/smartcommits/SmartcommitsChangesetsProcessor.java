package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;

public interface SmartcommitsChangesetsProcessor
{
    void startProcess(Progress forProgress, Repository repository, ChangesetService changesetService);
}
