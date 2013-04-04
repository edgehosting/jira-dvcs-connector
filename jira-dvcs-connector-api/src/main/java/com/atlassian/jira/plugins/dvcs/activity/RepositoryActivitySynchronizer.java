package com.atlassian.jira.plugins.dvcs.activity;

import com.atlassian.jira.plugins.dvcs.model.Progress;
import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryActivitySynchronizer
{
   void synchronize(Repository repository, Progress progress, boolean softSync);
}

