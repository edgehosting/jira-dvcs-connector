package com.atlassian.jira.plugins.dvcs.activity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryActivityDao
{
    // C-U-D
    RepositoryActivityPullRequestMapping saveActivity(Map<String, Object> activity);

    RepositoryPullRequestMapping savePullRequest(Map<String, Object> activity);

    /**
     * Updates issue keys related to the provided pull request to reflect current state.
     * 
     * @param pullRequestId
     */
    void updatePullRequestIssueKyes(int pullRequestId);

    void removeAll(final Repository forRepository);

    RepositoryActivityCommitMapping saveCommit(Map<String, Object> commit);

    void updateActivityStatus(int activityId, RepositoryActivityPullRequestUpdateMapping.Status status);

    // R
    List<RepositoryActivityPullRequestMapping> getRepositoryActivityForIssue(String issueKey);

    RepositoryPullRequestMapping findRequestById(int localId);

    RepositoryPullRequestMapping findRequestByRemoteId(int repositoryId, long remoteId);

    Set<String> getExistingIssueKeysMapping(Integer pullRequestId);

    RepositoryActivityCommitMapping getCommit(int pullRequesCommitId);

    RepositoryActivityCommitMapping getCommitByNode(int pullRequestId, String node);

    RepositoryActivityPullRequestUpdateMapping getPullRequestActivityByRemoteId(RepositoryPullRequestMapping pullRequest, String remoteId);

    List<RepositoryActivityPullRequestUpdateMapping> getPullRequestActivityByStatus(RepositoryPullRequestMapping pullRequest,
            RepositoryActivityPullRequestUpdateMapping.Status status);

    List<RepositoryActivityPullRequestCommentMapping> getPullRequestComments(RepositoryPullRequestMapping pullRequest);

}
