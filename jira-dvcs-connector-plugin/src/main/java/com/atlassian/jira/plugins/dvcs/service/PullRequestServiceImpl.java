package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping.Status;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.PullRequestTransformer;
import com.atlassian.jira.plugins.dvcs.event.PullRequestCreatedEvent;
import com.atlassian.jira.plugins.dvcs.event.PullRequestUpdatedEvent;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;

/**
 * Implementation of {@link PullRequestService}
 *
 * @since v1.4.4
 */
public class PullRequestServiceImpl implements PullRequestService
{
    private final RepositoryPullRequestDao pullRequestDao;
    private final PullRequestTransformer transformer;
    private final DvcsCommunicatorProvider dvcsCommunicatorProvider;
    private final ThreadEvents threadEvents;

    public PullRequestServiceImpl(final RepositoryPullRequestDao pullRequestDao,
            final RepositoryService repositoryService,
            final DvcsCommunicatorProvider dvcsCommunicatorProvider,
            final ThreadEvents threadEvents)
    {
        this.pullRequestDao = pullRequestDao;
        this.dvcsCommunicatorProvider = dvcsCommunicatorProvider;
        this.threadEvents = threadEvents;
        this.transformer = new PullRequestTransformer(repositoryService);
    }

    @Override
    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys)
    {
        return transform(pullRequestDao.getPullRequestsForIssue(issueKeys));
    }

    @Override
    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys, final String dvcsType)
    {
        return transform(pullRequestDao.getPullRequestsForIssue(issueKeys, dvcsType));
    }

    @Override
    public String getCreatePullRequestUrl(Repository repository, String sourceSlug, String sourceBranch, String destinationSlug, String destinationBranch, String eventSource)
    {
        DvcsCommunicator communicator = dvcsCommunicatorProvider.getCommunicator(repository.getDvcsType());
        return communicator.getCreatePullRequestUrl(repository, sourceSlug, sourceBranch, destinationSlug, destinationBranch, eventSource);
    }

    @Nonnull
    @Override
    public Set<String> getIssueKeys(int repositoryId, int pullRequestId)
    {
        return pullRequestDao.getIssueKeys(repositoryId, pullRequestId);
    }

    @Override
    public RepositoryPullRequestMapping createPullRequest(RepositoryPullRequestMapping repositoryPullRequestMapping)
    {
        RepositoryPullRequestMapping createdMapping = pullRequestDao.savePullRequest(repositoryPullRequestMapping);

        threadEvents.broadcast(new PullRequestCreatedEvent(transformer.transform(createdMapping)));
        return createdMapping;
    }

    @Override
    public RepositoryPullRequestMapping updatePullRequest(int pullRequestId, RepositoryPullRequestMapping updatedPullRequestMapping)
    {
        final RepositoryPullRequestMapping mappingBeforeUpdate = pullRequestDao.findRequestById(pullRequestId);
        if (mappingBeforeUpdate == null)
        {
            throw new IllegalArgumentException(String.format("RepositoryPullRequestMapping with id=%s does not exist", updatedPullRequestMapping.getID()));
        }

        RepositoryPullRequestMapping mappingAfterUpdate = pullRequestDao.updatePullRequestInfo(
                pullRequestId,
                updatedPullRequestMapping.getName(),
                updatedPullRequestMapping.getSourceBranch(),
                updatedPullRequestMapping.getDestinationBranch(),
                Status.valueOf(updatedPullRequestMapping.getLastStatus()),
                updatedPullRequestMapping.getUpdatedOn(),
                updatedPullRequestMapping.getSourceRepo(),
                updatedPullRequestMapping.getCommentCount()
        );

        // send both the before and after state of the PR in the event
        PullRequest prAfter = transformer.transform(mappingAfterUpdate);
        PullRequest prBefore = transformer.transform(mappingBeforeUpdate);
        threadEvents.broadcast(new PullRequestUpdatedEvent(prAfter, prBefore));

        return mappingAfterUpdate;
    }

    @Override
    public void updatePullRequestParticipants(final int pullRequestId, final int repositoryId, final Map<String, Participant> participantIndex)
    {
        PullRequestParticipantMapping[] oldParticipants = pullRequestDao.getParticipants(pullRequestId);
        for (PullRequestParticipantMapping participantMapping : oldParticipants)
        {
            Participant participant = participantIndex.remove(participantMapping.getUsername());
            if (participant == null)
            {
                pullRequestDao.removeParticipant(participantMapping);
            } else
            {
                boolean markedForSave = false;
                if (participant.isApproved() != participantMapping.isApproved())
                {
                    // update approval
                    participantMapping.setApproved(participant.isApproved());
                    markedForSave = true;
                }

                if (StringUtils.equals(participant.getRole(), participantMapping.getRole()))
                {
                    participantMapping.setRole(participant.getRole());
                    markedForSave = true;
                }

                if (markedForSave)
                {
                    pullRequestDao.saveParticipant(participantMapping);
                }
            }
        }

        for (String username : participantIndex.keySet())
        {
            Participant participant = participantIndex.get(username);
            pullRequestDao.createParticipant(pullRequestId, repositoryId, participant);
        }
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
