package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.annotations.PublicApi;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;

/**
 * Gets the pull requests for one or more issue keys or repository from connected dvcs account
 *
 */
@PublicApi
public interface DvcsPullRequestService
{
    /**
     * Find all pullRequests by one or more issue keys
     *
     * @param issueKeys the list of issue keys to find
     * @return list of (@link PullRequest}
     */
    List<PullRequest> getPullRequests(Iterable<String> issueKeys);

    String getCreatePullRequestUrl(Repository repository, String sourceSlug, String sourceBranch, String destinationSlug, String destinationBranch, String eventSource);

    List<PullRequest> getPullRequests(Iterable<String> issueKeys, String dvcsType);
}
