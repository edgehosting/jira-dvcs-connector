package com.atlassian.jira.plugins.dvcs.service.api;

import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.service.PullRequestService;
import com.google.common.collect.ImmutableList;

import java.util.List;

public class DvcsPullRequestServiceImpl implements DvcsPullRequestService
{
    private PullRequestService pullRequestService;

    public DvcsPullRequestServiceImpl(final PullRequestService pullRequestService)
    {
        this.pullRequestService = pullRequestService;
    }

    @Override
    public List<PullRequest> getPullRequests(final Iterable<String> issueKeys)
    {
        return ImmutableList.copyOf(pullRequestService.getByIssueKeys(issueKeys));
    }
}
