package com.atlassian.jira.plugins.dvcs.activity;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.atlassian.jira.plugins.dvcs.model.Repository;

public interface RepositoryActivityDao
{
    // C-U-D
    RepositoryPullRequestMapping savePullRequest(Repository domain, Map<String, Object> activity);

    RepositoryPullRequestMapping updatePullRequestInfo(int localId, String name, String sourceBranch, String dstBranch, String remoteStatus);

    /**
     * Updates issue keys related to commits of provided repository.
     *
     * @param domain
     */
    void updateCommitIssueKeys(Repository domain);

    /**
     * Updates issue keys related to the provided pull request to reflect current state.
     *
     * @param pullRequestId
     *
     * @return Number of found issues keys
     */
    int updatePullRequestIssueKeys(Repository domain, int pullRequestId);

    void removeAll(Repository domain);

    RepositoryCommitMapping saveCommit(Repository domain, Map<String, Object> commit);

    void linkCommit(Repository domain, RepositoryPullRequestMapping request, RepositoryCommitMapping commit);

    void unlinkCommit(Repository domain, RepositoryPullRequestMapping request, RepositoryCommitMapping commit);

    // R

    List<RepositoryPullRequestMapping> getPullRequestsForIssue(final Iterable<String> issueKeys);

    RepositoryPullRequestMapping findRequestById(int localId);

    RepositoryPullRequestMapping findRequestByRemoteId(Repository domain, long remoteId);

    Set<String> getExistingIssueKeysMapping(Repository domain, Integer pullRequestId);

    RepositoryCommitMapping getCommit(Repository domain, int pullRequesCommitId);

    RepositoryCommitMapping getCommitByNode(Repository domain, int pullRequestId, String node);

    RepositoryCommitMapping getCommitByNode(Repository domain, String node);


}
