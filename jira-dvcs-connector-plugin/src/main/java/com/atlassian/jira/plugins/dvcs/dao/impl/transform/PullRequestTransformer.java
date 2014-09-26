package com.atlassian.jira.plugins.dvcs.dao.impl.transform;

import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Changeset;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.PullRequestRef;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.Repository;
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

    public PullRequest transform(RepositoryPullRequestMapping pullRequestMapping, boolean withCommits)
    {
        if (pullRequestMapping == null)
        {
            return null;
        }

        Repository repository = repositoryService.get(pullRequestMapping.getToRepositoryId());

        final PullRequest pullRequest = new PullRequest(pullRequestMapping.getID());
        pullRequest.setRemoteId(pullRequestMapping.getRemoteId());
        pullRequest.setRepositoryId(pullRequestMapping.getToRepositoryId());
        pullRequest.setName(pullRequestMapping.getName());
        pullRequest.setUrl(pullRequestMapping.getUrl());

        pullRequest.setSource(new PullRequestRef(pullRequestMapping.getSourceBranch(), pullRequestMapping.getSourceRepo(), createRepositoryUrl(repository.getOrgHostUrl(), pullRequestMapping.getSourceRepo())));
        pullRequest.setDestination(new PullRequestRef(pullRequestMapping.getDestinationBranch(), createRepositoryLabel(repository), repository.getRepositoryUrl()));

        pullRequest.setStatus(PullRequestStatus.fromRepositoryPullRequestMapping(pullRequestMapping.getLastStatus()));
        pullRequest.setCreatedOn(pullRequestMapping.getCreatedOn());
        pullRequest.setUpdatedOn(pullRequestMapping.getUpdatedOn());
        pullRequest.setAuthor(pullRequestMapping.getAuthor());
        pullRequest.setParticipants(transform(pullRequestMapping.getParticipants()));
        pullRequest.setCommentCount(pullRequestMapping.getCommentCount());

        if (withCommits)
        {
            pullRequest.setCommits(transform(pullRequestMapping.getCommits()));
        }
        pullRequest.setExecutedBy(pullRequestMapping.getExecutedBy());
        return pullRequest;
    }

    private List<Participant> transform(final PullRequestParticipantMapping[] participantMappings)
    {
        if (participantMappings == null)
        {
            return null;
        }

        List<Participant> participants = new ArrayList<Participant>();
        for (PullRequestParticipantMapping participantMapping : participantMappings)
        {
            Participant participant = new Participant(participantMapping.getUsername(), participantMapping.isApproved(), participantMapping.getRole());
            participants.add(participant);
        }

        return participants;
    }

    private List<Changeset> transform(RepositoryCommitMapping[] commitMappings)
    {
        if (commitMappings == null)
        {
            return null;
        }

        List<Changeset> commits = new ArrayList<Changeset>();
        for (RepositoryCommitMapping commitMapping : commitMappings)
        {
            Changeset changeset = new Changeset(0, commitMapping.getNode(), commitMapping.getMessage(), commitMapping.getDate());
            changeset.setAuthor(commitMapping.getAuthor());
            changeset.setRawAuthor(commitMapping.getRawAuthor());
            commits.add(changeset);
        }

        return commits;
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
