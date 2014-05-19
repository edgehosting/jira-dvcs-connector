package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;

import java.util.List;
import java.util.Map;

/**
 * Service for manipulating with pull requests
 *
 * @since v1.4.4
 */
public interface PullRequestService
{
    List<PullRequest> getByIssueKeys(Iterable<String> issueKeys);

    List<PullRequest> getByIssueKeys(Iterable<String> issueKeys, boolean withCommits);

    String getCreatePullRequestUrl(Repository repository, String sourceSlug, String sourceBranch, String destinationSlug, String destinationBranch, String eventSource);

    List<PullRequest> getByIssueKeys(Iterable<String> issueKeys, String dvcsType);

    void updatePullRequestParticipants(int pullRequestId, int repositoryId, Map<String, Participant> participantIndex);

}
