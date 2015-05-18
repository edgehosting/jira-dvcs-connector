package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Service for manipulating and querying DVCS pull request data.
 *
 * @since v1.4.4
 */
public interface PullRequestService
{
    List<PullRequest> getByIssueKeys(Iterable<String> issueKeys);

    List<PullRequest> getByIssueKeys(Iterable<String> issueKeys, boolean withCommits);

    List<PullRequest> getByIssueKeys(Iterable<String> issueKeys, String dvcsType);

    String getCreatePullRequestUrl(Repository repository, String sourceSlug, String sourceBranch, String destinationSlug, String destinationBranch, String eventSource);

    /**
     * Retrieves keys of issues associated with the pull request. If either {@code repositoryId} or {@code
     * pullRequestId} point to non-existing entities, an empty set will be returned.
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

    /**
     * Creates a {@link RepositoryPullRequestMapping} in the database and raises a {@link com.atlassian.jira.plugins.dvcs.event.PullRequestCreatedEvent}.
     *
     *  This should really work with {@link PullRequest} but there's currently no straightforward way to convert one of
     * those into a RepositoryPullRequestMapping.
     *
     * @param repositoryPullRequestMapping the RepositoryPullRequestMapping to create
     * @since 2.1.6
     */
    RepositoryPullRequestMapping createPullRequest(RepositoryPullRequestMapping repositoryPullRequestMapping);

    /**
     * Updates a {@link RepositoryPullRequestMapping} in the database and raises {@link com.atlassian.jira.plugins.dvcs.event.PullRequestUpdatedEvent}.
     *
     * This should really take a {@link PullRequest} as its parameter but there's currently no straightforward way to
     * convert one of those into a RepositoryPullRequestMapping.
     * <p>
     * Note that this method does not attempt to compare the previous and current state of the pull request mapping: it
     * always performs the update and always raises an event.
     *
     * @param updatedPullRequestMapping a RepositoryPullRequestMapping
     * @since 2.1.6
     */
    RepositoryPullRequestMapping updatePullRequest(final int pullRequestId, RepositoryPullRequestMapping updatedPullRequestMapping);
}
