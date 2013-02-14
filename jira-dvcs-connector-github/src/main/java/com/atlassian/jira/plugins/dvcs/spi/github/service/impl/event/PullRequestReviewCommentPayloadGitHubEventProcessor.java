package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;

import org.eclipse.egit.github.core.CommitComment;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.client.GitHubRequest;
import org.eclipse.egit.github.core.client.RequestException;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PullRequestReviewCommentPayload;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequestLineComment;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubRepository;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubUser;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestLineCommentService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubRepositoryService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubUserService;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

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
     *      GitHubCommitService, GitHubUserService, GitHubRepositoryService, GithubClientProvider)
     */
    private final GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService;

    /**
     * @see #PullRequestReviewCommentPayloadGitHubEventProcessor(GitHubPullRequestLineCommentService, GitHubPullRequestService,
     *      GitHubCommitService, GitHubUserService, GitHubRepositoryService, GithubClientProvider)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #PullRequestReviewCommentPayloadGitHubEventProcessor(GitHubPullRequestLineCommentService, GitHubPullRequestService,
     *      GitHubCommitService, GitHubUserService, GitHubRepositoryService, GithubClientProvider)
     */
    private final GitHubCommitService gitHubCommitService;

    /**
     * @see #PullRequestReviewCommentPayloadGitHubEventProcessor(GitHubPullRequestLineCommentService, GitHubPullRequestService,
     *      GitHubCommitService, GitHubUserService, GitHubRepositoryService, GithubClientProvider)
     */
    private final GitHubUserService gitHubUserService;

    /**
     * @see #PullRequestReviewCommentPayloadGitHubEventProcessor(GitHubPullRequestLineCommentService, GitHubPullRequestService,
     *      GitHubCommitService, GitHubUserService, GitHubRepositoryService, GithubClientProvider)
     */
    private final GitHubRepositoryService gitHubRepositoryService;

    /**
     * @see #PullRequestReviewCommentPayloadGitHubEventProcessor(GitHubPullRequestLineCommentService, GitHubPullRequestService,
     *      GitHubCommitService, GitHubUserService, GitHubRepositoryService, GithubClientProvider)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * Constructor.
     * 
     * @param gitHubPullRequestLineCommentService
     *            injected {@link GitHubPullRequestLineCommentService} dependency
     * @param gitHubPullRequestService
     *            injected {@link GitHubPullRequestService} dependency
     * @param gitHubCommitService
     *            injected {@link GitHubCommitService} dependency
     * @param gitHubUserService
     *            injected {@link GitHubUserService} dependency
     * @param gitHubRepositoryService
     *            injected {@link GitHubRepositoryService} dependency
     * @param githubClientProvider
     *            injected {@link GithubClientProvider} dependency
     */
    public PullRequestReviewCommentPayloadGitHubEventProcessor( //
            GitHubPullRequestLineCommentService gitHubPullRequestLineCommentService, //
            GitHubPullRequestService gitHubPullRequestService, //
            GitHubCommitService gitHubCommitService, //
            GitHubUserService gitHubUserService, //
            GitHubRepositoryService gitHubRepositoryService, //
            GithubClientProvider githubClientProvider)
    {
        this.gitHubPullRequestLineCommentService = gitHubPullRequestLineCommentService;
        this.gitHubCommitService = gitHubCommitService;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.gitHubUserService = gitHubUserService;
        this.gitHubRepositoryService = gitHubRepositoryService;
        this.githubClientProvider = githubClientProvider;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository domainRepository, GitHubRepository domain, Event event)
    {
        PullRequestReviewCommentPayload payload = getPayload(event);
        CommitComment commitComment = payload.getComment();

        // is it already proceed?
        if (gitHubPullRequestLineCommentService.getByGitHubId(commitComment.getId()) != null)
        {
            return;
        }

        // not - will be proceed
        String repositryOwner = RepositoryId.createFromUrl(event.getRepo().getUrl()).getOwner();
        String repositoryName = event.getRepo().getName();
        long reposiotryGitHubId = event.getRepo().getId();
        GitHubRepository repository = gitHubRepositoryService.fetch(domainRepository, repositryOwner, repositoryName, reposiotryGitHubId);

        GitHubPullRequestLineComment gitHubPullRequestLineComment = new GitHubPullRequestLineComment();
        gitHubPullRequestLineComment.setDomain(domain);

        GitHubPullRequest pullRequest = getPullRequestByComment(domainRepository, repository, commitComment);
        GitHubCommit commit = gitHubCommitService.fetch(domainRepository, domain, repository, commitComment.getCommitId());
        GitHubUser createdBy = gitHubUserService.fetch(domainRepository, domain, payload.getComment().getUser().getLogin());

        // TODO: workaround to get access to the HTML URL of the comment
        String htmlUrl = "";
        try
        {
            GitHubRequest request = new GitHubRequest();
            request.setUri(new URL(commitComment.getUrl()).toURI().getRawPath());
            InputStream response = githubClientProvider.createClient(domainRepository).getStream(request);
            JsonElement element = new JsonParser().parse(new InputStreamReader(response, "UTF-8"));
            htmlUrl = element.getAsJsonObject().get("_links").getAsJsonObject().get("html").getAsJsonObject().get("href").getAsString();
            response.close();

        } catch (RequestException e)
        {
            if (e.getStatus() == 404)
            {
                // silently ignored, comment was already deleted
                return;

            }
        } catch (IOException e)
        {
            throw new RuntimeException(e);
        } catch (URISyntaxException e)
        {
            throw new RuntimeException(e);
        }
        //

        gitHubPullRequestLineComment.setGitHubId(commitComment.getId());
        gitHubPullRequestLineComment.setCreatedAt(commitComment.getCreatedAt());
        gitHubPullRequestLineComment.setCreatedBy(createdBy);
        gitHubPullRequestLineComment.setPullRequest(pullRequest);
        gitHubPullRequestLineComment.setUrl(commitComment.getUrl());
        gitHubPullRequestLineComment.setHtmlUrl(htmlUrl);
        gitHubPullRequestLineComment.setCommit(commit);
        gitHubPullRequestLineComment.setPath(commitComment.getPath());
        gitHubPullRequestLineComment.setLine(commitComment.getPosition());
        gitHubPullRequestLineComment.setText(commitComment.getBody());

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
     * @param domainRepository
     *            domain
     * @param domain
     *            owner of the comment
     * @param commitComment
     *            for which comment
     * @return resolved {@link GitHubPullRequest}
     */
    private GitHubPullRequest getPullRequestByComment(Repository domainRepository, GitHubRepository domain, CommitComment commitComment)
    {
        GitHubPullRequest result;

        RepositoryId repositoryId = RepositoryId.create(domainRepository.getOrgName(), domainRepository.getSlug());
        try
        {
            PullRequestService pullRequestService = githubClientProvider.getPullRequestService(domainRepository);

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
                            result = gitHubPullRequestService.fetch(domainRepository, domain, pullRequest.getId(), pullRequest.getNumber());
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
                            result = gitHubPullRequestService.fetch(domainRepository, domain, pullRequest.getId(), pullRequest.getNumber());
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
