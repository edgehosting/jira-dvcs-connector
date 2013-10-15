package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryActivityDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.PullRequestTransformer;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link PullRequestService}
 *
 * @since v1.4.4
 */
public class PullRequestServiceImpl implements PullRequestService
{
    private final RepositoryActivityDao pulLRequestDao;
    private final PullRequestTransformer transformer = new PullRequestTransformer();

    public PullRequestServiceImpl(final RepositoryActivityDao pulLRequestDao)
    {
        this.pulLRequestDao = pulLRequestDao;
    }

    @Override
    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys)
    {
        return transform(pulLRequestDao.getPullRequestsForIssue(issueKeys));
    }

    private List<PullRequest> transform(List<RepositoryPullRequestMapping> pullRequestsMappings)
    {
        List<PullRequest> pullRequests = new ArrayList<PullRequest>();

        for (RepositoryPullRequestMapping pullRequestMapping : pullRequestsMappings)
        {
            PullRequest pullRequest = transformer.transform(pullRequestMapping);
            if (pullRequest != null)
            {
                pullRequests.add(pullRequest);
            }
        }

        return pullRequests;
    }
}
