package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import java.util.Date;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryActivitySynchronizer
{

   void synchronize(Date lastSyncPoint, Repository forRepository);
    
}

