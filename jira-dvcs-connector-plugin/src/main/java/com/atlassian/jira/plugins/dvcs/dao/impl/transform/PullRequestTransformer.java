package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activity.PullRequestReviewerMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.PullRequestRef;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.model.Reviewer;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class PullRequestTransformer
{
    public static final Logger log = LoggerFactory.getLogger(PullRequestTransformer.class);

    private final RepositoryService repositoryService;

    public PullRequestTransformer(final RepositoryService repositoryService)
    {
        this.repositoryService = repositoryService;
    }

    public PullRequest transform(RepositoryPullRequestMapping pullRequestMapping)
    {
        if (pullRequestMapping == null)
        {
            return null;
        }

        Repository repository = repositoryService.get(pullRequestMapping.getToRepositoryId());

        final PullRequest pullRequest = new PullRequest(pullRequestMapping.getToRepositoryId());
        pullRequest.setRemoteId(pullRequestMapping.getRemoteId());
        pullRequest.setRepositoryId(pullRequestMapping.getToRepositoryId());
        pullRequest.setName(pullRequestMapping.getName());
        pullRequest.setUrl(pullRequestMapping.getUrl());

        pullRequest.setSource(new PullRequestRef(pullRequestMapping.getSourceBranch(), pullRequestMapping.getSourceRepo(), createRepositoryUrl(repository.getOrgHostUrl(), pullRequestMapping.getSourceRepo())));
        pullRequest.setDestination(new PullRequestRef(pullRequestMapping.getDestinationBranch(), createRepositoryLabel(repository), repository.getRepositoryUrl()));

        pullRequest.setStatus(pullRequestMapping.getLastStatus());
        pullRequest.setCreatedOn(pullRequestMapping.getCreatedOn());
        pullRequest.setUpdatedOn(pullRequestMapping.getUpdatedOn());
        pullRequest.setAuthor(pullRequestMapping.getAuthor());
        pullRequest.setReviewers(transform(pullRequestMapping.getReviewers()));

        return pullRequest;
    }

    private List<Reviewer> transform(final PullRequestReviewerMapping[] reviewerMappings)
    {
        if (reviewerMappings == null)
        {
            return null;
        }

        List<Reviewer> reviewers = new ArrayList<Reviewer>();
        for (PullRequestReviewerMapping reviewerMapping : reviewerMappings)
        {
            Reviewer reviewer = new Reviewer(reviewerMapping.getUsername(), reviewerMapping.isApproved(), reviewerMapping.getRole());
            reviewers.add(reviewer);
        }

        return  reviewers;
    }

    private String createRepositoryUrl(String hostUrl, String repositoryLabel)
    {
        if (repositoryLabel == null)
        {
            // the fork repository was deleted
            return null;
        }
        // normalize
        if (hostUrl != null && hostUrl.endsWith("/"))
        {
            hostUrl = hostUrl.substring(0, hostUrl.length() - 1);
        }
        return hostUrl + "/" + repositoryLabel;
    }

    private String createRepositoryLabel(Repository repository)
    {
        return repository.getOrgName() + "/" + repository.getSlug();
    }
}
