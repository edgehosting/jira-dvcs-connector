package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;

/**
 * TODO: Document this class / interface here
 *
 * @since v6.3
 */
public interface PullRequestClient
{
    String openPullRequest(String owner, String repositoryName, String password, String title, String description, String head, String base, String... reviewers);

    BitbucketPullRequest updatePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest, String title, String description, String base);

    BitbucketPullRequest openForkPullRequest(String owner, String repositoryName, String title, String description, String head, String base, String forkOwner, String forkPassword);

    /**
     * Closes provided pull request.
     *
     * @param owner
     *            repository owner
     * @param repositoryName
     *               repository name
     * @param pullRequest
     *            pull request to close
     */
    void declinePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest);

    /**
     * Approves pull request
     *
     * @param owner
     *                 repository owner
     * @param repositoryName
     *                 repository name
     * @param pullRequest
     *                  pull request to close
     */
    void approvePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest);

    /**
     * Merges pull request
     *
     * @param owner
     *                 repository owner
     * @param repositoryName
     *                 repository name
     * @param pullRequest
     *                 url of pull request to merge
     */
    void mergePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest);

    /**
     * Adds comment to provided pull request.
     *
     * @param pullRequest
     *            pull request
     * @param comment
     *            message
     * @return created remote comment
     */
    void commentPullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest, String comment);
}
