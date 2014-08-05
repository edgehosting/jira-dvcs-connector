package com.atlassian.jira.plugins.dvcs.smartcommits;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.ChangesetService;
import com.atlassian.util.concurrent.Promise;

import javax.annotation.Nonnull;

public interface SmartcommitsChangesetsProcessor
{
    @Nonnull
    Promise<Void> startProcess(Progress forProgress, Repository repository, ChangesetService changesetService);
}
