package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Gets the pull requests for one or more issue keys or repository from connected dvcs account
 *
 */
@PublicApi
public interface DvcsPullRequestService
{
    /**
     * Retrieves keys of issues associated with the pull request. If either {@code repositoryId} or
     * {@code pullRequestId} point to non-existing entities, an empty set will be returned.
     *
     * @param repositoryId id of the repository to query
     * @param pullRequestId id of the pull request to query
     * @return keys of issues associated with the pull request, or an empty set in case there were no matching issue
     * keys found.
     * @since v2.1.2
     */
    @Nonnull
    Set<String> getIssueKeys(int repositoryId, int pullRequestId);

    /**
     * Find all pullRequests by one or more issue keys
     *
     * @param issueKeys the list of issue keys to find
     * @return list of (@link PullRequest}
     */
    @Nonnull
    List<PullRequest> getPullRequests(Iterable<String> issueKeys);

    String getCreatePullRequestUrl(Repository repository, String sourceSlug, String sourceBranch, String destinationSlug, String destinationBranch, String eventSource);

    List<PullRequest> getPullRequests(Iterable<String> issueKeys, String dvcsType);
}
