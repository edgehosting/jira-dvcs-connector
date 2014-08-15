package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestResource;
import org.eclipse.egit.github.core.PullRequest;

public class GitHubPullRequestClient implements PullRequestClient<PullRequest>
{
    private final GitHubTestResource gitHubTestResource;

    public GitHubPullRequestClient(final GitHubTestResource gitHubTestResource)
    {
        this.gitHubTestResource = gitHubTestResource;
    }

    @Override
    public PullRequestDetails<PullRequest> openPullRequest(final String owner, final String repositoryName, final String password,
            final String title, final String description, final String head, final String base, final String... reviewers)
    {
        PullRequest pullRequest = gitHubTestResource.openPullRequest(owner, repositoryName, title, description, head, base);

        return new PullRequestDetails(pullRequest.getHtmlUrl(), new Long(pullRequest.getNumber()), pullRequest);
    }

    @Override
    public PullRequestDetails<PullRequest> updatePullRequest(final String owner, final String repositoryName,
            final String password, final PullRequest pullRequest, final String title, final String description, final String base)
    {

        PullRequest updatedPullRequest = gitHubTestResource.updatePullRequest(pullRequest, GitHubTestResource.USER, repositoryName, title,
                description, base);

        return new PullRequestDetails(updatedPullRequest.getHtmlUrl(), new Long(updatedPullRequest.getNumber()), updatedPullRequest);
    }

    @Override
    public PullRequestDetails<PullRequest> openForkPullRequest(final String owner, final String repositoryName, final String title, final String description, final String head, final String base, final String forkOwner, final String forkPassword)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void declinePullRequest(final String owner, final String repositoryName, final String password, final Long pullRequestId)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void approvePullRequest(final String owner, final String repositoryName, final String password, final Long pullRequestId)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void mergePullRequest(final String owner, final String repositoryName, final String password, final Long pullRequestId)
    {
        final int pullRequestNumber = pullRequestId.intValue();
        gitHubTestResource.mergePullRequest(owner, repositoryName, pullRequestNumber, "Merge Message");
    }

    @Override
    public void commentPullRequest(final String owner, final String repositoryName, final String password, final PullRequest pullRequest, final String comment)
    {
        gitHubTestResource.commentPullRequest(owner, repositoryName, pullRequest, comment);
    }
}
