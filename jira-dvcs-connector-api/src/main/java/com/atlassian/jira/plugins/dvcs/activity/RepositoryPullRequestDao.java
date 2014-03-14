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
     * Updates issue keys related to commits of provided repository.
     *
     * @param domain the repository
     */
    void updateCommitIssueKeys(Repository domain);

    /**
     * Updates issue keys related to the provided pull request to reflect current state.
     *
     * @param domain the repository
     * @param pullRequestId ID of the pull request
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

    List<RepositoryPullRequestMapping> getPullRequestsForIssue(Iterable<String> issueKeys, String dvcsType);

    RepositoryPullRequestMapping findRequestById(int localId);

    RepositoryPullRequestMapping findRequestByRemoteId(Repository domain, long remoteId);

    /**
     * @param repositoryId ID of the repository
     * @param pullRequestId ID of the pull request
     * @return keys of issues associated with the pull request
     * @since 2.1.1
     */
    Set<String> getIssueKeys(int repositoryId, int pullRequestId);

    /**
     *
     * @param domain the repository
     * @param pullRequestId pull request ID
     * @return keys of issues associated with the pull request
     * @deprecated use {@link #getIssueKeys(int, int)} instead
     */
    @Deprecated
    Set<String> getExistingIssueKeysMapping(Repository domain, Integer pullRequestId);

    RepositoryCommitMapping getCommit(Repository domain, int pullRequesCommitId);

    RepositoryCommitMapping getCommitByNode(Repository domain, int pullRequestId, String node);

    RepositoryCommitMapping getCommitByNode(Repository domain, String node);

    PullRequestParticipantMapping[] getParticipants(int pullRequestId);

    void removeParticipant(PullRequestParticipantMapping participantMapping);

    void saveParticipant(PullRequestParticipantMapping participantMapping);

    void createParticipant(int pullRequestId, int repositoryId, Participant participant);
}
