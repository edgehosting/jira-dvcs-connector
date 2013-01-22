package com.atlassian.jira.plugins.dvcs.spi.github.service.impl.event;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.egit.github.core.PullRequest;
import org.eclipse.egit.github.core.RepositoryCommit;
import org.eclipse.egit.github.core.RepositoryId;
import org.eclipse.egit.github.core.event.Event;
import org.eclipse.egit.github.core.event.PullRequestPayload;
import org.eclipse.egit.github.core.service.PullRequestService;

import com.atlassian.jira.plugins.dvcs.model.Repository;
import com.atlassian.jira.plugins.dvcs.spi.github.GithubClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubCommit;
import com.atlassian.jira.plugins.dvcs.spi.github.model.GitHubPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubCommitService;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubEventProcessor;
import com.atlassian.jira.plugins.dvcs.spi.github.service.GitHubPullRequestService;

/**
 * An {@link PullRequestPayload} based {@link GitHubEventProcessor}.
 * 
 * @author stanislav-dvorscak@solumiss.eu
 * 
 */
public class PullRequestPayloadGitHubEventProcessor extends AbstractGitHubEventProcessor<PullRequestPayload>
{

    /**
     * @see #PullRequestPayloadGitHubEventProcessor(GithubClientProvider, GitHubPullRequestService, GitHubPullRequestUpdateService,
     *      GitHubCommitService)
     */
    private final GithubClientProvider githubClientProvider;

    /**
     * @see #PullRequestPayloadGitHubEventProcessor(GithubClientProvider, GitHubPullRequestService, GitHubPullRequestUpdateService,
     *      GitHubCommitService)
     */
    private final GitHubPullRequestService gitHubPullRequestService;

    /**
     * @see #PullRequestPayloadGitHubEventProcessor(GithubClientProvider, GitHubPullRequestService, GitHubPullRequestUpdateService,
     *      GitHubCommitService)
     */
    private final GitHubCommitService gitHubCommitService;

    /**
     * Constructor.
     * 
     * @param githubClientProvider
     *            used for connection access
     * @param gitHubPullRequestService
     *            Injected {@link GitHubPullRequestService} dependency.
     * @param gitHubPullRequestUpdateService
     *            Injected {@link GitHubPullRequestUpdateService} dependency.
     * @param gitHubCommitService
     *            Injected {@link GitHubCommitService} dependency.
     */
    public PullRequestPayloadGitHubEventProcessor(GithubClientProvider githubClientProvider,
            GitHubPullRequestService gitHubPullRequestService, GitHubCommitService gitHubCommitService)
    {
        this.githubClientProvider = githubClientProvider;
        this.gitHubPullRequestService = gitHubPullRequestService;
        this.gitHubCommitService = gitHubCommitService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void process(Repository repository, Event event)
    {
        RepositoryId egitRepository = RepositoryId.createFromUrl(repository.getRepositoryUrl());
        PullRequestPayload payload = getPayload(event);
        PullRequest pullRequest = payload.getPullRequest();

        GitHubPullRequest gitHubPullRequest = gitHubPullRequestService.getByGitHubId(pullRequest.getId());
        if (gitHubPullRequest != null)
        {
            return;
        }

        List<GitHubCommit> initialCommits = new LinkedList<GitHubCommit>();
        try
        {
            PullRequestService pullRequestService = githubClientProvider.getPullRequestService(repository);
            List<RepositoryCommit> repositoryCommits = pullRequestService.getCommits(egitRepository, pullRequest.getCommits());
            for (RepositoryCommit repositoryCommit : repositoryCommits)
            {
                GitHubCommit commit = gitHubCommitService.getBySha(repositoryCommit.getSha());
                if (commit == null)
                {
                    commit = new GitHubCommit();
                    gitHubCommitService.map(commit, repositoryCommit.getCommit());
                    gitHubCommitService.save(commit);
                    initialCommits.add(commit);
                }
            }

        } catch (IOException e)
        {
            throw new RuntimeException(e);

        }

        // creates GitHubPullRequest
        gitHubPullRequest = new GitHubPullRequest();
        gitHubPullRequest.setTitle(pullRequest.getTitle());
        gitHubPullRequestService.save(gitHubPullRequest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<PullRequestPayload> getEventPayloadType()
    {
        return PullRequestPayload.class;
    }

}
