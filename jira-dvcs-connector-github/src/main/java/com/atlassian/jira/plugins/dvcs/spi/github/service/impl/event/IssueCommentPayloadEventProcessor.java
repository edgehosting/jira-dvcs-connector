package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubUserService;

/**
 * The {@link IssueCommentPayload} event processor.
 * 
 * @author Stanislav Dvorscak
 * 
 */
public class IssueCommentPayloadEventProcessor extends AbstractGitHubEventProcessor<IssueCommentPayload>
{

    /**
     * @see #IssueCommentPayloadEventProcessor(GitHubPullRequestCommentService, GitHubPullRequestService, GitHubUserService,
     *      GithubClientProvider)
     */
    private final GitHubPullRequestCommentService gitHubPullRequestCommentService;

    /**
     * @see #IssueCommentPayloadEventProcessor(GitHubPullRequestCommentService, GitHubPullRequestService, GitHubUserService,
     *      GithubClientProvider)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #IssueCommentPayloadEventProcessor(GitHubPullRequestCommentService, GitHubPullRequestService, GitHubUserService,
     *      GithubClientProvider)
     */
    private final GitHubUserService gitHubUserService;

    /**
     * @see #IssueCommentPayloadEventProcessor(GitHubPullRequestCommentService, GitHubPullRequestService, GitHubUserService,
     *      GithubClientProvider)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestCommentService
     *            Injected {@link GitHubPullRequestCommentService} dependency.
     * @param gitHubPullRequestService
     *            Injected {@link GitHubPullRequestService} dependency.
     * @param gitHubUserService
     *            Injected {@link GitHubUserService} dependency.
     * @param githubClientProvider
     *            Injected {@link GithubClientProvider} dependency.
     * 
     */
    public IssueCommentPayloadEventProcessor( //
            GitHubPullRequestCommentService gitHubPullRequestCommentService, //
            GitHubPullRequestService gitHubPullRequestService, //
            GitHubUserService gitHubUserService, //
            GithubClientProvider githubClientProvider //
    )
    {
        this.gitHubPullRequestCommentService = gitHubPullRequestCommentService;
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
        IssueCommentPayload payload = getPayload(event);
        Comment comment = payload.getComment();

        PullRequest pullRequest = payload.getIssue().getPullRequest();
        if (pullRequest != null && !StringUtils.isBlank(pullRequest.getHtmlUrl()))
        {
            GitHubPullRequest gitHubPullRequest = getPullRequestByHtmlUrl(repository, pullRequest.getHtmlUrl());
            if (gitHubPullRequest != null)
            {
                GitHubUser createdBy = gitHubUserService.fetch(comment.getUser().getLogin(), repository);
                GitHubPullRequestComment gitHubPullRequestComment = gitHubPullRequestCommentService.getByGitHubId(comment.getId());
                if (gitHubPullRequestComment == null)
                {
                    gitHubPullRequestComment = new GitHubPullRequestComment();
                }

                gitHubPullRequestCommentService.map(gitHubPullRequestComment, comment, gitHubPullRequest, createdBy);
                gitHubPullRequestCommentService.save(gitHubPullRequestComment);
            }
        }

    }

    /**
     * Resolves {@link GitHubPullRequest} for the provided pull request html URL.
     * 
     * @param repository
     *            over which repository
     * @param htmlUrl
     *            of the {@link PullRequest}
     * @return resolved {@link GitHubPullRequest}
     */
    private GitHubPullRequest getPullRequestByHtmlUrl(Repository repository, String htmlUrl)
    {
        GitHubPullRequest result;

        PullRequestService pullRequestService = githubClientProvider.getPullRequestService(repository);
        RepositoryId repositoryId = RepositoryId.create(repository.getOrgName(), repository.getSlug());

        try
        {
            // iterates over open pull requests
            for (PullRequest pullRequest : pullRequestService.getPullRequests(repositoryId, "open"))
            {
                if (pullRequest.getHtmlUrl().equals(htmlUrl))
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

            // iterates over closed pull-requests
            for (PullRequest pullRequest : pullRequestService.getPullRequests(repositoryId, "closed"))
            {
                if (pullRequest.getHtmlUrl().equals(htmlUrl))
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

            return null;

        } catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<IssueCommentPayload> getEventPayloadType()
    {
        return IssueCommentPayload.class;
    }

}
