package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.PullRequestRef;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PullRequestTransformer
{
    public static final Logger log = LoggerFactory.getLogger(PullRequestTransformer.class);

    public PullRequest transform(RepositoryPullRequestMapping pullRequestMapping)
    {
        if (pullRequestMapping == null)
        {
            return null;
        }

        final PullRequest pullRequest = new PullRequest(pullRequestMapping.getToRepositoryId());
        pullRequest.setRemoteId(pullRequestMapping.getRemoteId());
        pullRequest.setRepositoryId(pullRequestMapping.getToRepositoryId());
        pullRequest.setName(pullRequest.getName());
        pullRequest.setDescription(pullRequest.getDescription());
        pullRequest.setUrl(pullRequest.getUrl());

        pullRequest.setSource(new PullRequestRef(pullRequestMapping.getSourceBranch(), createRepository(pullRequestMapping.getSourceUrl())));
        pullRequest.setDestination(new PullRequestRef(pullRequestMapping.getDestinationBranch(), null));

        pullRequest.setStatus(pullRequestMapping.getLastStatus());
        pullRequest.setCreatedOn(pullRequestMapping.getCreatedOn());
        pullRequest.setAuthor(pullRequestMapping.getAuthor());

        return pullRequest;
    }

    private Repository createRepository(String url)
    {
        Repository repository = new Repository();
        repository.setRepositoryUrl(url);
        return repository;
    }
}
