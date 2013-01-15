package com.atlassian.jira.plugins.dvcs.activity;

import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryActivityDao
{
    void save (Map<String, Object> activity);

    List<RepositoryActivityPullRequestMapping> getRepositoryActivityForIssue(String issueKey);
    
    void removeAll (Repository forRepository);
    
}

