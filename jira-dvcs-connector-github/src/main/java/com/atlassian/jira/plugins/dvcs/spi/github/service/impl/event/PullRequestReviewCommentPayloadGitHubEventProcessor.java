package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import java.io.IOException;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestLineCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubUserService;

/**
 * The {@link PullRequestReviewCommentPayload} implementation of the {@link GitHubEventProcessor}.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class PullRequestReviewCommentPayloadGitHubEventProcessor extends AbstractGitHubEventProcessor<PullRequestReviewCommentPayload>
{

    /**
     * @see #PullRequestReviewCommentPayloadGitHubEventProcessor(GitHubPullRequestLineCommentService, GitHubPullRequestService,
     *      GitHubUserService, GitHubCommitService, GithubClientProvider)
     */
    private final GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService;

    /**
     * @see #PullRequestReviewCommentPayloadGitHubEventProcessor(GitHubPullRequestLineCommentService, GitHubPullRequestService,
     *      GitHubUserService, GitHubCommitService, GithubClientProvider)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #PullRequestReviewCommentPayloadGitHubEventProcessor(GitHubPullRequestLineCommentService, GitHubPullRequestService,
     *      GitHubUserService, GitHubCommitService, GithubClientProvider)
     */
    private final GitHubCommitService gitHubCommitService;

    /**
     * @see #PullRequestReviewCommentPayloadGitHubEventProcessor(GitHubPullRequestLineCommentService, GitHubPullRequestService,
     *      GitHubUserService, GitHubCommitService, GithubClientProvider)
     */
    private final GitHubUserService gitHubUserService;

    /**
     * @see #PullRequestReviewCommentPayloadGitHubEventProcessor(GitHubPullRequestLineCommentService, GitHubPullRequestService,
     *      GitHubUserService, GitHubCommitService, GithubClientProvider)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestLineCommentService
     *            Injected {@link GitHubPullRequestLineCommentService} dependency.
     * @param gitHubPullRequestService
     *            Injected {@link GitHubPullRequestService} dependency.
     * @param gitHubUserService
     *            Injected {@link GitHubUserService} dependency.
     * @param gitHubCommitService
     *            Injected {@link GitHubCommitService} dependency.
     * @param githubClientProvider
     *            Injected {@link GithubClientProvider} dependency.
     */
    public PullRequestReviewCommentPayloadGitHubEventProcessor( //
            GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService, //
            GitHubPullRequestService gitHubPullRequestService, //
            GitHubUserService gitHubUserService, //
            GitHubCommitService gitHubCommitService, //
            GithubClientProvider githubClientProvider)
    {
        this.gitHubPullRequestLineCommentService = gitHubPullRequestLineCommentService;
        this.gitHubCommitService = gitHubCommitService;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.gitHubUserService = gitHubUserService;
        this.githubClientProvider = githubClientProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, Event event)
    {
        PullRequestReviewCommentPayload payload = getPayload(event);
        CommitComment commitComment = payload.getComment();

        // is it already proceed?
        if (gitHubPullRequestLineCommentService.getByGitHubId(commitComment.getId()) != null)
        {
            return;
        }

        // not - will be proceed
        GitHubPullRequestLineComment gitHubPullRequestLineComment = new GitHubPullRequestLineComment();
        GitHubPullRequest pullRequest = getPullRequestByComment(repository, commitComment);
        GitHubCommit commit = gitHubCommitService.getBySha(commitComment.getCommitId());
        GitHubUser createdBy = gitHubUserService.fetch(payload.getComment().getUser().getLogin(), repository);
        gitHubPullRequestLineCommentService.map(gitHubPullRequestLineComment, commitComment, pullRequest, createdBy, commit);
        gitHubPullRequestLineCommentService.save(gitHubPullRequestLineComment);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PullRequestReviewCommentPayload> getEventPayloadType()
    {
        return PullRequestReviewCommentPayload.class;
    }

    /**
     * Resolves {@link GitHubPullRequest} for the provided comment, currently it is only workaround, because information is not available.
     * 
     * @param repository
     *            on which repository
     * @param commitComment
     *            for which comment
     * @return resolved {@link GitHubPullRequest}
     */
    private GitHubPullRequest getPullRequestByComment(Repository repository, CommitComment commitComment)
    {
        GitHubPullRequest result;

        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());
        try
        {
            PullRequestService pullRequestService = githubClientProvider.getPullRequestService(repository);

            // iterates over open pull requests
            for (PullRequest pullRequest : pullRequestService.getPullRequests(repositoryId, "open"))
            {
                for (CommitComment tmpCommitComment : pullRequestService.getComments(repositoryId, pullRequest.getNumber()))
                {
                    if (commitComment.getId() == tmpCommitComment.getId())
                    {
                        result = gitHubPullRequestService.getByGitHubId(pullRequest.getId());
                        if (result == null)
                        {
                            result = gitHubPullRequestService.fetch(repository, pullRequest.getId(), pullRequest.getNumber());
                            gitHubPullRequestService.save(result);
                        }

                        return result;
                    }
                }
            }

            // iterates over closed pull-requests
            for (PullRequest pullRequest : pullRequestService.getPullRequests(repositoryId, "closed"))
            {
                for (CommitComment tmpCommitComment : pullRequestService.getComments(repositoryId, pullRequest.getNumber()))
                {
                    if (commitComment.getId() == tmpCommitComment.getId())
                    {
                        result = gitHubPullRequestService.getByGitHubId(pullRequest.getId());
                        if (result == null)
                        {
                            result = gitHubPullRequestService.fetch(repository, pullRequest.getId(), pullRequest.getNumber());
                        }

                        return result;
                    }
                }
            }

            return null;

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }

    }

}
