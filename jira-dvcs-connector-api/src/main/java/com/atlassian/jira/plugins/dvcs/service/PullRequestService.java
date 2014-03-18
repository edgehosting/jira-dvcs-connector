package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service for manipulating and querying DVCS pull request data.
 *
 * @since v1.4.4
 */
public interface PullRequestService
{
    public List<PullRequest> getByIssueKeys(Iterable<String> issueKeys);

    List<PullRequest> getByIssueKeys(Iterable<String> issueKeys, String dvcsType);

    String getCreatePullRequestUrl(Repository repository, String sourceSlug, String sourceBranch, String destinationSlug, String destinationBranch, String eventSource);

    /**
     * Retrieves keys of issues associated with the pull request. If either {@code repositoryId} or
     * {@code pullRequestId} point to non-existing entities, an empty set will be returned.
     *
     * @param repositoryId id of the repository to query
     * @param pullRequestId id of the pull request to query
     * @return keys of issues associated with the pull request, or an empty set in case there were no matching issue
     * keys found.
     * @since v2.1.1
     */
    @Nonnull
    Set<String> getIssueKeys(int repositoryId, int pullRequestId);

    void updatePullRequestParticipants(int pullRequestId, int repositoryId, Map<String, Participant> participantIndex);

}
