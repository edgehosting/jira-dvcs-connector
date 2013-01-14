package com.atlassian.jira.plugins.dvcs.spi.bitbucket.activity;

import java.util.Date;
import java.util.List;
import java.util.Map;

public interface RepositoryActivityDao
{
    void save (Map<String, Object> activity);

    List<RepositoryActivity> getRepositoryActivityForIssue(String issueKey);
    
    Date getLastSynchronizationDate(Integer forRepository);
}

