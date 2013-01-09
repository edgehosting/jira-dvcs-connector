package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import java.util.Date;
import java.util.List;

public interface RepositoryActivityDao
{
    List<RepositoryActivity> getRepositoryActivityForIssue(String issueKey);
    
    Date getLastSynchronizationDate(Integer forRepository);
}

