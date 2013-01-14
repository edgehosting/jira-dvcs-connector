package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryActivitySynchronizer
{

   void synchronize(Repository forRepository);
    
}

