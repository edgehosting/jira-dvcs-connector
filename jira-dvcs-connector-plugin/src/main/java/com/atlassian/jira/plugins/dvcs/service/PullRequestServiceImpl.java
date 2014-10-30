package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.PullRequestTransformer;
import com.atlassian.jira.plugins.dvcs.event.PullRequestCreatedEvent;
import com.atlassian.jira.plugins.dvcs.event.PullRequestUpdatedEvent;
import com.atlassian.jira.plugins.dvcs.event.ThreadEvents;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import static com.atlassian.jira.plugins.dvcs.model.PullRequestStatus.OPEN;

/**
 * Implementation of {@link PullRequestService}
 *
 * @since v1.4.4
 */
@Component
public class PullRequestServiceImpl implements PullRequestService
{
    @Resource
    private RepositoryPullRequestDao pullRequestDao;

    private PullRequestTransformer transformer;

    @Resource
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Resource
    private RepositoryService repositoryService;

    @Resource
    private ThreadEvents threadEvents;

    @PostConstruct
    public void init()
    {
        transformer = new PullRequestTransformer(repositoryService);
    }

    @Override
    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys)
    {
        return transform(pullRequestDao.getByIssueKeys(issueKeys), false);
    }

    @Override
    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys, final String dvcsType)
    {
        return transform(pullRequestDao.getByIssueKeys(issueKeys, dvcsType), false);
    }

    @Override
    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys, final boolean withCommits)
    {
        return transform(pullRequestDao.getByIssueKeys(issueKeys), withCommits);
    }

    private List<PullRequest> transform(List<RepositoryPullRequestMapping> pullRequestsMappings, boolean withCommits)
    {
        List<PullRequest> pullRequests = new ArrayList<PullRequest>();

        for (RepositoryPullRequestMapping pullRequestMapping : pullRequestsMappings)
        {
            PullRequest pullRequest = transformer.transform(pullRequestMapping, withCommits);
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

    @Nonnull
    @Override
    public Set<String> getIssueKeys(int repositoryId, int pullRequestId)
    {
        return pullRequestDao.getIssueKeys(repositoryId, pullRequestId);
    }

    @Override
    public RepositoryPullRequestMapping createPullRequest(RepositoryPullRequestMapping repositoryPullRequestMapping)
    {
        final RepositoryPullRequestMapping createdMapping = pullRequestDao.savePullRequest(repositoryPullRequestMapping);
        final PullRequest pullRequest = transformer.transform(createdMapping, false);

        // we know that pull requests always start off in the OPEN state so if that's not the current state we can
        // deduce that we missed the pull request's creation. in this case we broadcast separate create and updated
        // events so that listeners can observe the distinct events.
        if (OPEN != pullRequest.getStatus())
        {
            final PullRequest createdPullRequest = transformer.transform(createdMapping, false);
            createdPullRequest.setStatus(OPEN);

            threadEvents.broadcast(new PullRequestCreatedEvent(createdPullRequest));
            threadEvents.broadcast(new PullRequestUpdatedEvent(pullRequest, createdPullRequest));
        }
        else
        {
            threadEvents.broadcast(new PullRequestCreatedEvent(pullRequest));
        }

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

        RepositoryPullRequestMapping mappingAfterUpdate = pullRequestDao.updatePullRequestInfo(pullRequestId, updatedPullRequestMapping);

        // send both the before and after state of the PR in the event
        PullRequest prAfter = transformer.transform(mappingAfterUpdate, false);
        PullRequest prBefore = transformer.transform(mappingBeforeUpdate, false);

        if (isPullRequestReopened(prBefore, prAfter))
        {
            prAfter.setExecutedBy(null); // clear misleading author set in executedBy field for re-opened since it won't be available cheaply via Github api
        }

        threadEvents.broadcast(new PullRequestUpdatedEvent(prAfter, prBefore));
        return mappingAfterUpdate;
    }

    private boolean isPullRequestReopened(PullRequest prBefore, PullRequest prAfter)
    {
        return (prAfter.getStatus() == PullRequestStatus.OPEN && prBefore.getStatus() != PullRequestStatus.OPEN);
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
            }
            else
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
}
