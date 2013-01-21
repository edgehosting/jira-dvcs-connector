package com.atlassian.jira.plugins.dvcs.activity;

import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryActivityDao
{
    // C-U-D
    void saveActivity (Map<String, Object> activity);

    RepositoryPullRequestMapping savePullRequest (Map<String, Object> activity);

    void removeAll (Repository forRepository);
    
    // R
    List<RepositoryActivityPullRequestMapping> getRepositoryActivityForIssue(String issueKey);
    
    RepositoryPullRequestMapping findRequestById(Integer localId, String repoSlug);

    
}

