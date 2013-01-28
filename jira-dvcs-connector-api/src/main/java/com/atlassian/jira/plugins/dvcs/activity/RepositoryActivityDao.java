package com.atlassian.jira.plugins.dvcs.activity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryActivityDao
{
    // C-U-D
    void saveActivity (Map<String, Object> activity);

    RepositoryPullRequestMapping savePullRequest (Map<String, Object> activity, Set<String> issueKeys);
    
    void saveIssueKeysMappings(Collection<String> issueKeys, int id);
    
    void removeAll(final Repository forRepository);

    // R
    List<RepositoryActivityPullRequestMapping> getRepositoryActivityForIssue(String issueKey);
    
    RepositoryPullRequestMapping findRequestById(Integer localId, String repoSlug);

    Set<String> getExistingIssueKeysMapping(Integer pullRequestId);

}

