package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

/**
 * This interface provides a Facade for common pull request operations on our supported repository hosts - Bitbucket and
 * Github. Not all the parameters are necessarily used by all the implementations, implementations need to do enough to
 * support testing.
 *
 * Like {@link it.restart.com.atlassian.jira.plugins.dvcs.testClient.Dvcs} there will be some points where the
 * underlying remote repository will not fit exactly, i.e. Github does not support approval of Pull Requests but
 * it should be enough to support testing.
 */
public interface PullRequestClient<T>
{
    PullRequestDetails<T> openPullRequest(String owner, String repositoryName, String password, String title, String description, String head, String base, String... reviewers);

    PullRequestDetails<T> updatePullRequest(String owner, String repositoryName, String password, T pullRequest, String title, String description, String base);

    PullRequestDetails<T> openForkPullRequest(String owner, String repositoryName, String title, String description, String head, String base, String forkOwner, String forkPassword);

    /**
     * Closes provided pull request.
     *
     * @param owner repository owner
     * @param repositoryName repository name
     * @param pullRequestId pull request to close
     */
    void declinePullRequest(String owner, String repositoryName, String password, Long pullRequestId);

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
     * @param pullRequest pull request on which we are commenting
     * @param comment message
     * @return created remote comment
     */
    void commentPullRequest(String owner, String repositoryName, String password, T pullRequest, String comment);

    /**
     * Represents a pull request
     */
    public static class PullRequestDetails<P>
    {

        private String location;
        private Long id;
        private P pullRequest;

        public PullRequestDetails(final String location, final Long id, final P pullRequest)
        {
            this.location = location;
            this.id = id;
            this.pullRequest = pullRequest;
        }

        public String getLocation()
        {
            return location;
        }

        public Long getId()
        {
            return id;
        }

        public P getPullRequest()
        {
            return pullRequest;
        }
    }
}
