package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
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
    private final RepositoryPullRequestDao pulLRequestDao;

    private final PullRequestTransformer transformer;

    public PullRequestServiceImpl(final RepositoryPullRequestDao pulLRequestDao, final RepositoryService repositoryService)
    {
        this.pulLRequestDao = pulLRequestDao;
        transformer = new PullRequestTransformer(repositoryService);
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
