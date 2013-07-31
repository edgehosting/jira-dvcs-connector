package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.jira.plugins.dvcs.sync.Synchronizer;

@PublicApi
public interface SmartcommitsChangesetsProcessor
{
    void startProcess(Synchronizer synchronizer, Repository repository, ChangesetService changesetService);
}
