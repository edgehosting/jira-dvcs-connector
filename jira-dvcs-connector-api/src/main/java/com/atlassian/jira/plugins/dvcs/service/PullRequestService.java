package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;

/**
 * Service for manipulating with pull requests
 *
 * @since v1.4.4
 */
public interface PullRequestService
{
    public List<PullRequest> getByIssueKeys(Iterable<String> issueKeys);

    String getCreatePullRequestUrl(Repository repository, String sourceSlug, String sourceBranch, String destinationSlug, String destinationBranch, String eventSource);

    List<PullRequest> getByIssueKeys(Iterable<String> issueKeys, String dvcsType);
}
