package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;

public interface PullRequestClient
{
    PullRequestDetails openPullRequest(String owner, String repositoryName, String password, String title, String description, String head, String base, String... reviewers);

    PullRequestDetails updatePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest, String title, String description, String base);

    PullRequestDetails openForkPullRequest(String owner, String repositoryName, String title, String description, String head, String base, String forkOwner, String forkPassword);

    /**
     * Closes provided pull request.
     *
     * @param owner repository owner
     * @param repositoryName repository name
     * @param pullRequest pull request to close
     */
    void declinePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest);

    /**
     * Approves pull request
     *
     * @param owner repository owner
     * @param repositoryName repository name
     * @param pullRequestId Id of the pull request to approve
     */
    void approvePullRequest(String owner, String repositoryName, String password, Long pullRequestId);

    /**
     * Merges pull request
     *
     * @param owner repository owner
     * @param repositoryName repository name
     * @param pullRequestId Id of pull request to merge
     */
    void mergePullRequest(String owner, String repositoryName, String password, Long pullRequestId);

    /**
     * Adds comment to provided pull request.
     *
     * @param pullRequest pull request
     * @param comment message
     * @return created remote comment
     */
    void commentPullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest, String comment);

    public static class PullRequestDetails
    {

        private String location;
        private Long id;

        public PullRequestDetails(final String location, final Long id)
        {
            this.location = location;
            this.id = id;
        }

        public String getLocation()
        {
            return location;
        }

        public Long getId()
        {
            return id;
        }
    }
}
