package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.plugins.dvcs.base.resource.GitHubTestResource;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import org.eclipse.egit.github.core.PullRequest;

public class GitHubPullRequestClient implements PullRequestClient
{
    private final GitHubTestResource gitHubTestResource;

    public GitHubPullRequestClient(final GitHubTestResource gitHubTestResource)
    {
        this.gitHubTestResource = gitHubTestResource;
    }

    @Override
    public String openPullRequest(final String owner, final String repositoryName, final String password,
            final String title, final String description, final String head, final String base, final String... reviewers)
    {
        PullRequest pullRequest = gitHubTestResource.openPullRequest(owner, repositoryName, title, description, head, base);

        return pullRequest.getHtmlUrl();
    }

    @Override
    public BitbucketPullRequest updatePullRequest(final String owner, final String repositoryName, final String password, final BitbucketPullRequest pullRequest, final String title, final String description, final String base)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public BitbucketPullRequest openForkPullRequest(final String owner, final String repositoryName, final String title, final String description, final String head, final String base, final String forkOwner, final String forkPassword)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void declinePullRequest(final String owner, final String repositoryName, final String password, final BitbucketPullRequest pullRequest)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void approvePullRequest(final String owner, final String repositoryName, final String password, final BitbucketPullRequest pullRequest)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void mergePullRequest(final String owner, final String repositoryName, final String password, final BitbucketPullRequest pullRequest)
    {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void commentPullRequest(final String owner, final String repositoryName, final String password, final BitbucketPullRequest pullRequest, final String comment)
    {
        throw new UnsupportedOperationException("Not implemented");
    }
}
