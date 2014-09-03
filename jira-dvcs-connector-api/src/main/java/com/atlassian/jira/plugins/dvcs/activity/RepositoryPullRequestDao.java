package com.atlassian.jira.plugins.dvcs.activity;

import com.atlassian.jira.plugins.dvcs.dao.IssueToMappingClosure;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface RepositoryPullRequestDao
{
    // C-U-D

    /**
     * @return a new RepositoryPullRequestMapping
     * @see #savePullRequest(RepositoryPullRequestMapping)
     */
    RepositoryPullRequestMapping createPullRequest();

    /**
     * Do not use this nasty method.
     *
     * @deprecated In favour of {@link #savePullRequest(RepositoryPullRequestMapping)}.
     */
    @Deprecated
    RepositoryPullRequestMapping savePullRequest(Repository domain, Map<String, Object> activity);

    /**
     * Saves a new RepositoryPullRequestMapping.
     *
     * @param pullRequest the RepositoryPullRequestMapping to save
     * @return a saved RepositoryPullRequestMapping
     * @see #createPullRequest()
     * @since 2.1.6
     */
    RepositoryPullRequestMapping savePullRequest(RepositoryPullRequestMapping pullRequest);

    RepositoryPullRequestMapping updatePullRequestInfo(int localId, RepositoryPullRequestMapping pullRequestMapping);

    /**
     * Updates issue keys related to the provided pull request to reflect current state.
     *
     * @param domain the repository
     * @param pullRequestId ID of the pull request
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

    List<RepositoryPullRequestMapping> getPullRequestsForIssue(Iterable<String> issueKeys, String dvcsType);

    RepositoryPullRequestMapping findRequestById(int localId);

    RepositoryPullRequestMapping findRequestByRemoteId(Repository domain, long remoteId);

    /**
     * Retrieves keys of issues associated with the pull request. If either {@code repositoryId} or {@code
     * pullRequestId} point to non-existing entities, an empty set will be returned.
     *
     * @param repositoryId ID of the repository
     * @param pullRequestId ID of the pull request
     * @return keys of issues associated with the pull request, or an empty set in case there were no matching issue
     * keys found.
     * @since v2.1.1
     */
    Set<String> getIssueKeys(int repositoryId, int pullRequestId);

    /**
     * Retrieves keys of issues associated with the pull request. If either {@code domain.id} or {@code pullRequestId}
     * point to non-existing entities, an empty set will be returned.
     *
     * @param domain the repository
     * @param pullRequestId pull request ID
     * @return keys of issues associated with the pull request, or an empty set in case there were no matching issue
     * keys found.
     * @deprecated in v2.1.1, use {@link #getIssueKeys(int, int)} instead
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

    int getNumberOfDistinctIssueKeysToPullRequests();

    boolean forEachIssueKeyToPullRequest(IssueToMappingClosure closure);
}
