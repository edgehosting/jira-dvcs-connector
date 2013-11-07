package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.PullRequestTransformer;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;

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

    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;

    public PullRequestServiceImpl(final RepositoryPullRequestDao pulLRequestDao, final RepositoryService repositoryService, final DvcsCommunicatorProvider dvcsCommunicatorProvider)
    {
        this.pulLRequestDao = pulLRequestDao;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
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

    @Override
    public String getCreatePullRequestUrl(Repository repository, String sourceSlug, String sourceBranch, String destinationSlug, String destinationBranch, String eventSource)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getCreatePullRequestUrl(repository, sourceSlug, sourceBranch, destinationSlug, destinationBranch, eventSource);
    }
}
