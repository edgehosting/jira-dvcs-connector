package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;
import org.eclipse.egit.github.core.Comment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.IssueCommentPayload;
import org.eclipse.egit.github.core.service.PullRequestService;
import org.springframework.beans.factory.annotation.Qualifier;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
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
            @Qualifier("githubClientProvider") GithubClientProvider githubClientProvider //
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
    public void process(Repository domainRepository, GitHubRepository domain, Event event)
    {
        IssueCommentPayload payload = getPayload(event);
        Comment comment = payload.getComment();

        PullRequest pullRequest = payload.getIssue().getPullRequest();
        if (pullRequest != null && !StringUtils.isBlank(pullRequest.getHtmlUrl()))
        {
            GitHubPullRequest gitHubPullRequest = getPullRequestByHtmlUrl(domainRepository, domain, pullRequest.getHtmlUrl());
            if (gitHubPullRequest != null)
            {
                GitHubUser createdBy = gitHubUserService.fetch(domainRepository, domain, comment.getUser().getLogin());
                GitHubPullRequestComment gitHubPullRequestComment = gitHubPullRequestCommentService.getByGitHubId(comment.getId());
                if (gitHubPullRequestComment == null)
                {
                    gitHubPullRequestComment = new GitHubPullRequestComment();
                }

                gitHubPullRequestComment.setDomain(domain);
                
                gitHubPullRequestComment.setGitHubId(comment.getId());
                gitHubPullRequestComment.setPullRequest(gitHubPullRequest);
                gitHubPullRequestComment.setCreatedAt(comment.getCreatedAt());
                gitHubPullRequestComment.setCreatedBy(createdBy);
                gitHubPullRequestComment.setText(comment.getBody());

                gitHubPullRequestCommentService.save(gitHubPullRequestComment);
            }
        }

    }

    /**
     * Resolves {@link GitHubPullRequest} for the provided pull request html URL.
     * 
     * @param domain
     *            over which repository
     * @param domainRepository
     *            over which repository
     * @param htmlUrl
     *            of the {@link PullRequest}
     * @return resolved {@link GitHubPullRequest}
     */
    private GitHubPullRequest getPullRequestByHtmlUrl(Repository domainRepository, GitHubRepository domain, String htmlUrl)
    {
        GitHubPullRequest result;

        PullRequestService pullRequestService = githubClientProvider.getPullRequestService(domainRepository);
        RepositoryId repositoryId = RepositoryId.create(domainRepository.getOrgName(), domainRepository.getSlug());

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
                        result = gitHubPullRequestService.fetch(domainRepository, domain, pullRequest.getId(), pullRequest.getNumber());
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
                        result = gitHubPullRequestService.fetch(domainRepository, domain, pullRequest.getId(), pullRequest.getNumber());
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
