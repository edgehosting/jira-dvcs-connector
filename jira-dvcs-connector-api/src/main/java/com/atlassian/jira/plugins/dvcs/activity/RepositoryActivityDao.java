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

    RepositoryActivityCommitMapping saveCommit(Map<String,Object> commit);
    
    void updateActivityStatus(int activityId, RepositoryActivityPullRequestUpdateMapping.Status status);
    
    // R
    List<RepositoryActivityPullRequestMapping> getRepositoryActivityForIssue(String issueKey);
    
    RepositoryPullRequestMapping findRequestById(int localId);
    
    RepositoryPullRequestMapping findRequestByRemoteId(int repositoryId, long remoteId);

    Set<String> getExistingIssueKeysMapping(Integer pullRequestId);

	List<RepositoryActivityCommitMapping> getCommits(List<Integer> pullRequesCommitIds);

	RepositoryActivityPullRequestCommentMapping findCommentByRemoteId(int repositoryId, long remoteId);

	RepositoryActivityPullRequestLineCommentMapping findLineCommentByRemoteId(int repositoryId, long remoteId);

	RepositoryActivityPullRequestCommentMapping getComment(int id);
	
	List<RepositoryActivityPullRequestUpdateMapping> getByPullRequestStatus(RepositoryPullRequestMapping pullRequest, RepositoryActivityPullRequestUpdateMapping.Status status);

    RepositoryActivityPullRequestLineCommentMapping getLineComment(int id);

}

