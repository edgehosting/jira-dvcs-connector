package com.atlassian.jira.plugins.dvcs.sync;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Resource;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryCommitMapping;
import com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestMapping;
import com.atlassian.jira.plugins.dvcs.model.Message;
import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.service.RepositoryService;
import com.atlassian.jira.plugins.dvcs.service.message.MessageAddress;
import com.atlassian.jira.plugins.dvcs.service.message.MessageConsumer;
import com.atlassian.jira.plugins.dvcs.service.message.MessagingService;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.message.GitHubPullRequestSynchronizeMessage;

/**
 * Message consumer {@link GitHubPullRequestSynchronizeMessage}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class GitHubPullRequestSynchronizeMessageConsumer implements MessageConsumer<GitHubPullRequestSynchronizeMessage>
{

    /**
     * Logger of this class.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubPullRequestSynchronizeMessageConsumer.class);

    /**
     * @see #getQueue()
     */
    public static final String QUEUE = GitHubPullRequestSynchronizeMessageConsumer.class.getCanonicalName();

    /**
     * @see #getAddress()
     */
    public static final String ADDRESS = GitHubPullRequestSynchronizeMessage.class.getCanonicalName();

    /**
     * Injected {@link com.atlassian.jira.plugins.dvcs.activity.RepositoryPullRequestDao} dependency.
     */
    @Resource
    private RepositoryPullRequestDao repositoryPullRequestDao;

    /**
     * Injected {@link RepositoryService} dependency.
     */
    @Resource
    private RepositoryService repositoryService;

    /**
     * Injected {@link GithubClientProvider} dependency.
     */
    @Resource(name = "githubClientProvider")
    private GithubClientProvider gitHubClientProvider;

    /**
     * Injected {@link MessagingService} dependency.
     */
    @Resource
    private MessagingService messagingService;

    /**
     * {@inheritDoc}
     */
    @Override
    public String getQueue()
    {
        return QUEUE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onReceive(Message<GitHubPullRequestSynchronizeMessage> message, GitHubPullRequestSynchronizeMessage payload)
    {
        Repository repository = payload.getRepository();
        PullRequest remotePullRequest = getRemotePullRequest(repository, payload.getPullRequestNumber());
        RepositoryPullRequestMapping localPullRequest = updateLocalPullRequest(repository, remotePullRequest);
        repositoryPullRequestDao.updatePullRequestIssueKeys(repository, localPullRequest.getID());
        updateLocalPullRequestCommits(repository, remotePullRequest, localPullRequest);
    }

    /**
     * Creates or updates local version of remote {@link PullRequest}.
     * 
     * @param repository
     *            pull request owner
     * @param remotePullRequest
     *            remote pull request representation
     * @return created/updated local pull request
     */
    private RepositoryPullRequestMapping updateLocalPullRequest(Repository repository, PullRequest remotePullRequest)
    {
        RepositoryPullRequestMapping localPullRequest = repositoryPullRequestDao.findRequestByRemoteId(repository, remotePullRequest.getId());
        if (localPullRequest == null)
        {
            Map<String, Object> activity = new HashMap<String, Object>();
            map(activity, repository, remotePullRequest);
            localPullRequest = repositoryPullRequestDao.savePullRequest(repository, activity);
        } else
        {
            repositoryPullRequestDao.updatePullRequestInfo(localPullRequest.getID(), remotePullRequest.getTitle(), remotePullRequest.getBase()
                    .getRef(), remotePullRequest.getHead().getRef(), resolveStatus(remotePullRequest), remotePullRequest
                    .getUpdatedAt(), getRepositoryFullName(remotePullRequest.getBase().getRepo()));
        }
        return localPullRequest;
    }

    private void updateLocalPullRequestCommits(Repository repository, PullRequest remotePullRequest,
            RepositoryPullRequestMapping localPullRequest)
    {
        List<RepositoryCommit> remoteCommits = getRemotePullRequestCommits(repository, remotePullRequest);

        Set<RepositoryCommitMapping> remainingCommitsToDelete = new HashSet<RepositoryCommitMapping>(Arrays.asList(localPullRequest
                .getCommits()));

        for (RepositoryCommit remoteCommit : remoteCommits)
        {
            RepositoryCommitMapping commit = repositoryPullRequestDao.getCommitByNode(repository, remoteCommit.getSha());
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

        for (RepositoryCommitMapping commit : remainingCommitsToDelete)
        {
            repositoryPullRequestDao.unlinkCommit(repository, localPullRequest, commit);
        }
    }

    /**
     * Loads remote {@link PullRequest}.
     * 
     * @param repository
     *            owner of pull request
     * @param number
     *            number of pull request
     * @return remote pull request
     */
    private PullRequest getRemotePullRequest(Repository repository, int number)
    {
        try
        {
            PullRequestService pullRequestService = gitHubClientProvider.getPullRequestService(repository);
            return pullRequestService.getPullRequest(RepositoryId.createFromUrl(repository.getRepositoryUrl()), number);
        } catch (IOException e)
        {
            throw new RuntimeException(e);
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

    private void map(Map<String, Object> target, Repository repository, PullRequest source)
    {
        target.put(RepositoryPullRequestMapping.REMOTE_ID, source.getId());
        target.put(RepositoryPullRequestMapping.NAME, source.getTitle());

        target.put(RepositoryPullRequestMapping.URL, source.getHtmlUrl());
        target.put(RepositoryPullRequestMapping.TO_REPO_ID, repository.getId());

        target.put(RepositoryPullRequestMapping.AUTHOR, source.getUser().getLogin());
        target.put(RepositoryPullRequestMapping.CREATED_ON, source.getCreatedAt());
        target.put(RepositoryPullRequestMapping.UPDATED_ON, source.getUpdatedAt());
        target.put(RepositoryPullRequestMapping.SOURCE_REPO, getRepositoryFullName(source.getBase().getRepo()));
        target.put(RepositoryPullRequestMapping.SOURCE_BRANCH, source.getBase().getRef());
        target.put(RepositoryPullRequestMapping.DESTINATION_BRANCH, source.getHead().getRef());
        target.put(RepositoryPullRequestMapping.LAST_STATUS, resolveStatus(source).name());
    }

    private void map(Map<String, Object> target, RepositoryCommit source)
    {
        target.put(RepositoryCommitMapping.RAW_AUTHOR, source.getCommit().getAuthor().getName());
        target.put(RepositoryCommitMapping.MESSAGE, source.getCommit().getMessage());
        target.put(RepositoryCommitMapping.NODE, source.getCommit().getSha());
        target.put(RepositoryCommitMapping.DATE, source.getCommit().getAuthor().getDate());
    }

    private RepositoryPullRequestMapping.Status resolveStatus(PullRequest pullRequest)
    {
        if ("opened".equalsIgnoreCase(pullRequest.getState()))
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
        return gitHubRepository.getOwner().getLogin() + "/" + gitHubRepository.getName();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public MessageAddress<GitHubPullRequestSynchronizeMessage> getAddress()
    {
        return messagingService.get(GitHubPullRequestSynchronizeMessage.class, ADDRESS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getParallelThreads()
    {
        return MessageConsumer.THREADS_PER_CONSUMER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shouldDiscard(int messageId, int retryCount, GitHubPullRequestSynchronizeMessage payload, String[] tags)
    {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void afterDiscard(int messageId, int retryCount, GitHubPullRequestSynchronizeMessage payload, String[] tags)
    {
    }

}
