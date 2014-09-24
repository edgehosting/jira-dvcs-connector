package com.atlassian.jira.plugins.dvcs.base.resource.github;

import com.atlassian.fugue.Either;
import org.eclipse.egit.github.core.MergeStatus;
import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.Repository;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.service.PullRequestService;

import java.io.IOException;
import java.util.List;

/**
 * Convenience wrapper around {@link org.eclipse.egit.github.core.service.PullRequestService} that masks IO exceptions
 * and works on a single repository
 */
public class EGitPullRequestServiceWrapper
{
    private final Repository repository;
    private final PullRequestService pullRequestService;

    public EGitPullRequestServiceWrapper(final Repository repository, final PullRequestService pullRequestService)
    {
        this.repository = repository;
        this.pullRequestService = pullRequestService;
    }

    public PullRequest create(PullRequest request)
    {
        try
        {
            return pullRequestService.createPullRequest(repository, request);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public MergeStatus merge(int id, String commitMessage)
    {
        try
        {
            return pullRequestService.merge(repository, id, commitMessage);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public PullRequest edit(PullRequest request)
    {
        try
        {
            return pullRequestService.editPullRequest(repository, request);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public PullRequest get(int id)
    {
        try
        {
            return pullRequestService.getPullRequest(repository, id);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    public Either<IOException, PullRequest> getAsEither(int id)
    {
        try
        {
            return Either.right(pullRequestService.getPullRequest(repository, id));
        }
        catch (IOException e)
        {
            return Either.left(e);
        }
    }

    public List<RepositoryCommit> getCommits(int id)
    {
        try
        {
            return pullRequestService.getCommits(repository, id);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
