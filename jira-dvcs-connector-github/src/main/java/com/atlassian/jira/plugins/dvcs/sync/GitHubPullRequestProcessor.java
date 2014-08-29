package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.event.IssuesChangedEvent;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.NotificationService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import org.apache.commons.collections.CollectionUtils;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.PullRequestMarker;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.User;
import org.eclipse.egit.github.core.service.IssueService;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.Resource;

/**
 * Processes GitHub PullRequest
 */
@Component
public class GitHubPullRequestProcessor
{
    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPullRequestProcessor.class);

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao} dependency.
     */
    @Resource
    private RepositoryPullRequestDao repositoryPullRequestDao;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.service.PullRequestService} dependency.
     */
    @Resource
    private com.atlassian.jira.plugins.dvcs.service.PullRequestService pullRequestService;

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider} dependency.
     */
    @Resource (name = "githubClientProvider")
    private GithubClientProvider gitHubClientProvider;

    @Resource
    private NotificationService notificationService;

    /**
     * Updates pull request if update date differs
     *
     * @return <i>true</i> if updated, <i>false</i> otherwise
     */
    public boolean processPullRequestIfNeeded(final Repository repository, final PullRequest remotePullRequest)
    {
        RepositoryPullRequestMapping localPullRequest = repositoryPullRequestDao.findRequestByRemoteId(repository,
                remotePullRequest.getNumber());


        if (localPullRequest == null || remotePullRequest.getUpdatedAt().after(localPullRequest.getUpdatedOn()))
        {
            processPullRequest(repository, remotePullRequest, localPullRequest);
            return true;
        }

        return false;
    }

    public void processPullRequest(final Repository repository, final PullRequest remotePullRequest)
    {
        RepositoryPullRequestMapping localPullRequest = repositoryPullRequestDao.findRequestByRemoteId(repository,
                remotePullRequest.getNumber());

        processPullRequest(repository, remotePullRequest, localPullRequest);
    }

    public void processPullRequest(final Repository repository, final PullRequest remotePullRequest, RepositoryPullRequestMapping localPullRequest)
    {
        Map<String, Participant> participantIndex = new HashMap<String, Participant>();

        try
        {
            localPullRequest = updateLocalPullRequest(repository, remotePullRequest, localPullRequest, participantIndex);
        }
        catch (IllegalStateException e)
        {
            // This should not happen
            LOGGER.warn("Pull request " + remotePullRequest.getId() + " from repository with id " + repository.getId() + " could not be processed", e);
            // let's return prematurely
            return;
        }

        Set<String> oldIssueKeys = repositoryPullRequestDao.getIssueKeys(repository.getId(), localPullRequest.getID());

        repositoryPullRequestDao.updatePullRequestIssueKeys(repository, localPullRequest.getID());

        processPullRequestComments(repository, remotePullRequest, localPullRequest, participantIndex);
        processPullRequestReviewComments(repository, remotePullRequest, localPullRequest, participantIndex);

        pullRequestService.updatePullRequestParticipants(localPullRequest.getID(), repository.getId(), participantIndex);

        Set<String> newIssueKeys = repositoryPullRequestDao.getIssueKeys(repository.getId(), localPullRequest.getID());
        ImmutableSet<String> allIssueKeys = ImmutableSet.<String>builder().addAll(newIssueKeys).addAll(oldIssueKeys).build();
        notificationService.broadcast(new IssuesChangedEvent(repository.getId(), repository.getDvcsType(), allIssueKeys));
    }

    /**
     * Creates or updates local version of remote {@link PullRequest}.
     *
     * @param repository pull request owner
     * @param remotePullRequest remote pull request representation
     * @return created/updated local pull request
     */
    private RepositoryPullRequestMapping updateLocalPullRequest(Repository repository, PullRequest remotePullRequest,
            RepositoryPullRequestMapping localPullRequest, Map<String, Participant> participantIndex)
    {
        boolean shouldUpdateCommits = false;
        if (localPullRequest == null)
        {
            shouldUpdateCommits = true;
            localPullRequest = pullRequestService.createPullRequest(toDaoModelPullRequest(repository, remotePullRequest, null));
        }
        else
        {
            shouldUpdateCommits = shouldCommitsBeLoaded(remotePullRequest, localPullRequest);
            localPullRequest = pullRequestService.updatePullRequest(localPullRequest.getID(), toDaoModelPullRequest(repository, remotePullRequest, localPullRequest));
        }

        addParticipant(participantIndex, remotePullRequest.getUser(), Participant.ROLE_PARTICIPANT);
        addParticipant(participantIndex, remotePullRequest.getMergedBy(), Participant.ROLE_REVIEWER);
        addParticipant(participantIndex, remotePullRequest.getAssignee(), Participant.ROLE_REVIEWER);

        if (shouldUpdateCommits)
        {
            updateLocalPullRequestCommits(repository, remotePullRequest, localPullRequest);
        }

        return localPullRequest;
    }

    private boolean shouldCommitsBeLoaded(PullRequest remote, RepositoryPullRequestMapping local)
    {
        return hasStatusChanged(remote, local) || hasSourceChanged(remote, local) || hasDestinationChanged(remote, local);
    }

    private boolean hasStatusChanged(PullRequest remote, RepositoryPullRequestMapping local)
    {
        return !resolveStatus(remote).name().equals(local.getLastStatus());
    }

    private boolean hasSourceChanged(PullRequest remote, RepositoryPullRequestMapping local)
    {
        return !Objects.equal(local.getSourceBranch(), getBranchName(remote.getHead(), local.getSourceBranch()))
                || !Objects.equal(local.getSourceRepo(), getRepositoryFullName(remote.getHead()));
    }

    private boolean hasDestinationChanged(PullRequest remote, RepositoryPullRequestMapping local)
    {
        return !Objects.equal(local.getDestinationBranch(), getBranchName(remote.getBase(), local.getDestinationBranch()));
    }

    private String checkNotNull(String branch, String object)
    {
        if (branch == null)
        {
            throw new IllegalStateException(object + " must not be null");
        }

        return branch;
    }

    private String getBranchName(PullRequestMarker ref, String oldBranchName)
    {
        if (ref == null || ref.getRef() == null)
        {
            return oldBranchName;
        }

        return ref.getRef();
    }

    private void addParticipant(Map<String, Participant> participantIndex, User user, String role)
    {
        if (user != null)
        {
            Participant participant = participantIndex.get(user.getLogin());

            if (participant == null)
            {
                participantIndex.put(user.getLogin(), new Participant(user.getLogin(), false, role));
            }
        }
    }

    private void updateLocalPullRequestCommits(Repository repository, PullRequest remotePullRequest,
            RepositoryPullRequestMapping localPullRequest)
    {
        List<RepositoryCommit> remoteCommits = getRemotePullRequestCommits(repository, remotePullRequest);

        Set<RepositoryCommitMapping> remainingCommitsToDelete = new HashSet<RepositoryCommitMapping>(Arrays.asList(localPullRequest
                .getCommits()));

        final Map<String, RepositoryCommitMapping> commitsIndex = Maps.uniqueIndex(remainingCommitsToDelete, new Function<RepositoryCommitMapping, String>()
        {
            @Override
            public String apply(@Nullable final RepositoryCommitMapping repositoryCommitMapping)
            {
                return repositoryCommitMapping.getNode();
            }
        });

        for (RepositoryCommit remoteCommit : remoteCommits)
        {
            RepositoryCommitMapping commit = commitsIndex.get(getSha(remoteCommit));
            if (commit == null)
            {
                Map<String, Object> commitData = new HashMap<String, Object>();
                map(commitData, remoteCommit);
                commit = repositoryPullRequestDao.saveCommit(repository, commitData);
                repositoryPullRequestDao.linkCommit(repository, localPullRequest, commit);
            }
            else
            {
                remainingCommitsToDelete.remove(commit);
            }
        }

        if (!CollectionUtils.isEmpty(remainingCommitsToDelete))
        {
            LOGGER.debug("Removing commit in pull request {}", localPullRequest.getID());
            repositoryPullRequestDao.unlinkCommits(repository, localPullRequest, remainingCommitsToDelete);
            repositoryPullRequestDao.removeCommits(remainingCommitsToDelete);
        }
    }

    /**
     * Loads remote commits for provided pull request.
     *
     * @param repository pull request owner
     * @param remotePullRequest remote pull request
     * @return remote commits of pull request
     */
    private List<RepositoryCommit> getRemotePullRequestCommits(Repository repository, PullRequest remotePullRequest)
    {
        PullRequestService pullRequestService = gitHubClientProvider.getPullRequestService(repository);
        try
        {
            return pullRequestService.getCommits(RepositoryId.createFromUrl(repository.getRepositoryUrl()), remotePullRequest.getNumber());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes comments of a Pull Request.
     */
    private void processPullRequestComments(Repository repository, PullRequest remotePullRequest,
            RepositoryPullRequestMapping localPullRequest, Map<String, Participant> participantIndex)
    {
        updateCommentsCount(remotePullRequest, localPullRequest);

        IssueService issueService = gitHubClientProvider.getIssueService(repository);
        RepositoryId repositoryId = RepositoryId.createFromUrl(repository.getRepositoryUrl());
        List<Comment> pullRequestComments;
        try
        {
            pullRequestComments = issueService.getComments(repositoryId, remotePullRequest.getNumber());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        remotePullRequest.setComments(Math.max(remotePullRequest.getComments(), pullRequestComments.size()));

        for (Comment comment : pullRequestComments)
        {
            addParticipant(participantIndex, comment.getUser(), Participant.ROLE_PARTICIPANT);
        }
    }

    /**
     * Processes review comments of a Pull Request.
     */
    private void processPullRequestReviewComments(Repository repository, PullRequest remotePullRequest,
            RepositoryPullRequestMapping localPullRequest, Map<String, Participant> participantIndex)
    {
        updateCommentsCount(remotePullRequest, localPullRequest);

        PullRequestService pullRequestService = gitHubClientProvider.getPullRequestService(repository);
        RepositoryId repositoryId = RepositoryId.createFromUrl(repository.getRepositoryUrl());
        List<CommitComment> pullRequestReviewComments;
        try
        {
            pullRequestReviewComments = pullRequestService.getComments(repositoryId, remotePullRequest.getNumber());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        remotePullRequest.setReviewComments(Math.max(remotePullRequest.getReviewComments(), pullRequestReviewComments.size()));

        for (CommitComment comment : pullRequestReviewComments)
        {
            addParticipant(participantIndex, comment.getUser(), Participant.ROLE_PARTICIPANT);
        }
    }

    private void updateCommentsCount(PullRequest remotePullRequest, RepositoryPullRequestMapping localPullRequest)
    {
        localPullRequest.setCommentCount(remotePullRequest.getComments() + remotePullRequest.getReviewComments());

        // updates count
        pullRequestService.updatePullRequest(localPullRequest.getID(), localPullRequest);
    }

    @VisibleForTesting
    RepositoryPullRequestMapping toDaoModelPullRequest(Repository repository, PullRequest source, RepositoryPullRequestMapping localPullRequest)
    {
        String sourceBranch = checkNotNull(getBranchName(source.getHead(), localPullRequest != null ? localPullRequest.getSourceBranch() : null), "Source branch");
        String dstBranch = checkNotNull(getBranchName(source.getBase(), localPullRequest != null ? localPullRequest.getDestinationBranch() : null), "Destination branch");

        PullRequestStatus prStatus = resolveStatus(source);

        RepositoryPullRequestMapping target = repositoryPullRequestDao.createPullRequest();
        target.setDomainId(repository.getId());
        target.setRemoteId((long) source.getNumber());
        target.setName(ActiveObjectsUtils.stripToLimit(source.getTitle(), 255));

        target.setUrl(source.getHtmlUrl());
        target.setToRepositoryId(repository.getId());

        target.setAuthor(source.getUser() != null ? source.getUser().getLogin() : null);
        target.setCreatedOn(source.getCreatedAt());
        target.setUpdatedOn(source.getUpdatedAt());
        target.setSourceRepo(getRepositoryFullName(source.getHead()));
        target.setSourceBranch(sourceBranch);
        target.setDestinationBranch(dstBranch);
        target.setLastStatus(prStatus.name());
        target.setCommentCount(source.getComments());

        if (prStatus == PullRequestStatus.OPEN)
        {
            target.setExecutedBy(target.getAuthor());
        }
        else
        {
            // Note: PullRequest.mergedBy will have the user who merged the PR but will miss who closed or reopened.
            target.setExecutedBy(source.getMergedBy() != null ? source.getMergedBy().getLogin() : null);
        }

        return target;
    }

    private void map(Map<String, Object> target, RepositoryCommit source)
    {
        target.put(RepositoryCommitMapping.RAW_AUTHOR, source.getCommit().getAuthor().getName());
        target.put(RepositoryCommitMapping.MESSAGE, source.getCommit().getMessage());
        target.put(RepositoryCommitMapping.NODE, getSha(source));
        target.put(RepositoryCommitMapping.DATE, source.getCommit().getAuthor().getDate());
        target.put(RepositoryCommitMapping.MERGE, source.getParents() != null && source.getParents().size() > 1);
    }

    private String getSha(final RepositoryCommit source)
    {
        return source.getSha() != null ? source.getSha() : source.getCommit().getSha();
    }

    private PullRequestStatus resolveStatus(PullRequest pullRequest)
    {
        PullRequestStatus githubStatus = PullRequestStatus.fromGithubStatus(pullRequest.getState(), pullRequest.getMergedAt());

        if (githubStatus == null)
        {
            LOGGER.warn("Unable to parse Status of GitHub Pull Request, unknown GH state: " + pullRequest.getState());
            return PullRequestStatus.OPEN;
        }

        return githubStatus;
    }

    private String getRepositoryFullName(PullRequestMarker pullRequestMarker)
    {
        if (pullRequestMarker == null)
        {
            return null;
        }

        final org.eclipse.egit.github.core.Repository gitHubRepository = pullRequestMarker.getRepo();
        if (gitHubRepository == null || gitHubRepository.getOwner() == null)
        {
            return null;
        }

        return gitHubRepository.getOwner().getLogin() + "/" + gitHubRepository.getName();
    }
}
