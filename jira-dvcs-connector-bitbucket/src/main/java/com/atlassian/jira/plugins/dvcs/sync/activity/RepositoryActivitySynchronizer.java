package com.atlassian.jira.plugins.dvcs.sync.activity;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryActivitySynchronizer
{

   void synchronize(Repository forRepository, boolean softSync);
    
}

