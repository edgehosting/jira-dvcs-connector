package com.atlassian.jira.plugins.dvcs.sync;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Participant;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.util.ActiveObjectsUtils;
import com.google.common.base.Function;
import com.google.common.base.Objects;
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
 *
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
    @Resource(name = "githubClientProvider")
    private GithubClientProvider gitHubClientProvider;

    /**
     * Updates pull request if update date differs
     *
     * @param repository
     * @param remotePullRequest
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
        Map<String, Participant> participantIndex = new HashMap<String,Participant>();

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

        repositoryPullRequestDao.updatePullRequestIssueKeys(repository, localPullRequest.getID());

        processPullRequestComments(repository, remotePullRequest, localPullRequest, participantIndex);
        processPullRequestReviewComments(repository, remotePullRequest, localPullRequest, participantIndex);

        pullRequestService.updatePullRequestParticipants(localPullRequest.getID(), repository.getId(), participantIndex);
    }

    /**
     * Creates or updates local version of remote {@link PullRequest}.
     *
     * @param repository
     *            pull request owner
     * @param remotePullRequest
     *            remote pull request representation
     * @param localPullRequest
     * @return created/updated local pull request
     */
    private RepositoryPullRequestMapping updateLocalPullRequest(Repository repository, PullRequest remotePullRequest,
            RepositoryPullRequestMapping localPullRequest, Map<String, Participant> participantIndex)
    {
        boolean shouldUpdateCommits = false;
        if (localPullRequest == null)
        {
            shouldUpdateCommits = true;
            Map<String, Object> activity = new HashMap<String, Object>();
            map(activity, repository, remotePullRequest);
            localPullRequest = repositoryPullRequestDao.savePullRequest(repository, activity);
        }
        else
        {
            String sourceBranch = checkNotNull(getBranchName(remotePullRequest.getHead(), localPullRequest.getSourceBranch()), "Source branch");
            String dstBranch = checkNotNull(getBranchName(remotePullRequest.getBase(), localPullRequest.getDestinationBranch()), "Destination branch");

            shouldUpdateCommits = shouldCommitsBeLoaded(remotePullRequest, localPullRequest);

            localPullRequest = repositoryPullRequestDao.updatePullRequestInfo(localPullRequest.getID(), remotePullRequest.getTitle(),
                    sourceBranch, dstBranch, resolveStatus(remotePullRequest), remotePullRequest
                            .getUpdatedAt(), getRepositoryFullName(remotePullRequest.getHead().getRepo()), remotePullRequest.getComments());
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
                || !Objects.equal(local.getSourceRepo(), getRepositoryFullName(remote.getHead().getRepo()));
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
            } else
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
     * @param repository
     *            pull request owner
     * @param remotePullRequest
     *            remote pull request
     * @return remote commits of pull request
     */
    private List<RepositoryCommit> getRemotePullRequestCommits(Repository repository, PullRequest remotePullRequest)
    {
        PullRequestService pullRequestService = gitHubClientProvider.getPullRequestService(repository);
        try
        {
            return pullRequestService.getCommits(RepositoryId.createFromUrl(repository.getRepositoryUrl()), remotePullRequest.getNumber());
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * Processes comments of a Pull Request.
     *
     * @param repository
     * @param remotePullRequest
     * @param localPullRequest
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
        } catch (IOException e)
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
     *
     * @param repository
     * @param remotePullRequest
     * @param localPullRequest
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
        } catch (IOException e)
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
        int commentsCount = remotePullRequest.getComments() + remotePullRequest.getReviewComments();
        // updates count
        repositoryPullRequestDao.updatePullRequestInfo(localPullRequest.getID(), localPullRequest.getName(),
                localPullRequest.getSourceBranch(), localPullRequest.getDestinationBranch(),
                RepositoryPullRequestMapping.Status.valueOf(localPullRequest.getLastStatus()), localPullRequest.getUpdatedOn(),
                localPullRequest.getSourceRepo(), commentsCount);
    }

    private void map(Map<String, Object> target, Repository repository, PullRequest source)
    {
        String sourceBranch = checkNotNull(getBranchName(source.getHead(), null), "Source branch");
        String dstBranch = checkNotNull(getBranchName(source.getBase(), null), "Destination branch");

        target.put(RepositoryPullRequestMapping.REMOTE_ID, Long.valueOf(source.getNumber()));
        target.put(RepositoryPullRequestMapping.NAME, ActiveObjectsUtils.stripToLimit(source.getTitle(), 255));

        target.put(RepositoryPullRequestMapping.URL, source.getHtmlUrl());
        target.put(RepositoryPullRequestMapping.TO_REPO_ID, repository.getId());

        if (source.getUser() != null)
        {
            target.put(RepositoryPullRequestMapping.AUTHOR, source.getUser().getLogin());
        }
        target.put(RepositoryPullRequestMapping.CREATED_ON, source.getCreatedAt());
        target.put(RepositoryPullRequestMapping.UPDATED_ON, source.getUpdatedAt());
        target.put(RepositoryPullRequestMapping.SOURCE_REPO, getRepositoryFullName(source.getHead().getRepo()));
        target.put(RepositoryPullRequestMapping.SOURCE_BRANCH, sourceBranch);
        target.put(RepositoryPullRequestMapping.DESTINATION_BRANCH, dstBranch);
        target.put(RepositoryPullRequestMapping.LAST_STATUS, resolveStatus(source).name());
        target.put(RepositoryPullRequestMapping.COMMENT_COUNT, source.getComments());
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

    private RepositoryPullRequestMapping.Status resolveStatus(PullRequest pullRequest)
    {
        if ("open".equalsIgnoreCase(pullRequest.getState()))
        {
            return RepositoryPullRequestMapping.Status.OPEN;
        } else if ("closed".equalsIgnoreCase(pullRequest.getState()))
        {
            return pullRequest.getMergedAt() != null ? RepositoryPullRequestMapping.Status.MERGED
                    : RepositoryPullRequestMapping.Status.DECLINED;
        } else
        {
            LOGGER.warn("Unable to parse Status of GitHub Pull Request, unknown GH state: " + pullRequest.getState());
            return RepositoryPullRequestMapping.Status.OPEN;
        }
    }

    private String getRepositoryFullName(org.eclipse.egit.github.core.Repository gitHubRepository)
    {
        if (gitHubRepository == null || gitHubRepository.getOwner() == null)
        {
            return null;
        }

        return gitHubRepository.getOwner().getLogin() + "/" + gitHubRepository.getName();
    }
}
