package com.atlassian.jira.plugins.dvcs.activity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryActivityDao
{
    // C-U-D
	RepositoryActivityPullRequestMapping saveActivity (Map<String, Object> activity);

    RepositoryPullRequestMapping savePullRequest (Map<String, Object> activity, Set<String> issueKeys);
    
    void saveIssueKeysMappings(Collection<String> issueKeys, int id);
    
    void removeAll(final Repository forRepository);

    void saveCommit(Map<String,Object> commit);
    
    // R
    List<RepositoryActivityPullRequestMapping> getRepositoryActivityForIssue(String issueKey);
    
    RepositoryPullRequestMapping findRequestById(int localId);
    
    RepositoryPullRequestMapping findRequestByRemoteId(int repositoryId, int remoteId);

    Set<String> getExistingIssueKeysMapping(Integer pullRequestId);
}

