package com.atlassian.jira.plugins.dvcs.service;

import com.atlassian.jira.plugins.dvcs.activity.PullRequestParticipantMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.dao.impl.transform.PullRequestTransformer;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequest;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicator;
import com.atlassian.jira.plugins.dvcs.service.remote.DvcsCommunicatorProvider;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Implementation of {@link PullRequestService}
 *
 * @since v1.4.4
 */
public class PullRequestServiceImpl implements PullRequestService
{
    @Resource
    private RepositoryPullRequestDao pullRequestDao;

    private PullRequestTransformer transformer;

    @Resource
    private DvcsCommunicatorProvider dvcsCommunicatorProvider;

    @Resource
    private RepositoryService repositoryService;

    @PostConstruct
    public void init()
    {
        transformer = new PullRequestTransformer(repositoryService);
    }

    @Override
    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys)
    {
        return transform(pullRequestDao.getPullRequestsForIssue(issueKeys), false);
    }

    @Override
    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys, final String dvcsType)
    {
        return transform(pullRequestDao.getPullRequestsForIssue(issueKeys, dvcsType), false);
    }

    @Override
    public List<PullRequest> getByIssueKeys(final Iterable<String> issueKeys, final boolean withCommits)
    {
        return transform(pullRequestDao.getPullRequestsForIssue(issueKeys), withCommits);
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
}
