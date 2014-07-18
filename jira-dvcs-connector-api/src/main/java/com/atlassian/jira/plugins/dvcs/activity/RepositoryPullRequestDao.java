package com.atlassian.jira.plugins.dvcs.activity;

import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RepositoryPullRequestDao
{
    // C-U-D
    RepositoryPullRequestMapping savePullRequest(Repository domain, Map<String, Object> activity);

    RepositoryPullRequestMapping updatePullRequestInfo(int localId, String name, String sourceBranch, String dstBranch, RepositoryPullRequestMapping.Status status,
            Date updatedOn, String sourceRepo, final int commentCount);

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

    void unlinkCommits(Repository domain, RepositoryPullRequestMapping request, Iterable<? extends RepositoryCommitMapping> commits);

    void removeCommits(Iterable<? extends RepositoryCommitMapping> commits);

    // R

    List<RepositoryPullRequestMapping> getPullRequestsForIssue(final Iterable<String> issueKeys);

    RepositoryPullRequestMapping findRequestById(int localId);

    RepositoryPullRequestMapping findRequestByRemoteId(Repository domain, long remoteId);

    Set<String> getExistingIssueKeysMapping(Repository domain, Integer pullRequestId);

    RepositoryCommitMapping getCommit(Repository domain, int pullRequesCommitId);

    RepositoryCommitMapping getCommitByNode(Repository domain, int pullRequestId, String node);

    RepositoryCommitMapping getCommitByNode(Repository domain, String node);

    PullRequestParticipantMapping[] getParticipants(int pullRequestId);

    void removeParticipant(PullRequestParticipantMapping participantMapping);

    void saveParticipant(PullRequestParticipantMapping participantMapping);

    void createParticipant(int pullRequestId, int repositoryId, Participant participant);

    List<RepositoryPullRequestMapping> getPullRequestsForIssue(Iterable<String> issueKeys, String dvcsType);
}
