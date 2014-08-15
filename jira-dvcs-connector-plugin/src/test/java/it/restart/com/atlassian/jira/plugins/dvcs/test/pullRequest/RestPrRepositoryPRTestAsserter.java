package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPullRequest;
import org.testng.Assert;

public class RestPrRepositoryPRTestAsserter
{
    private String repositoryName;
    private String pullRequestLocation;
    private String pullRequestName;
    private String username;
    private String fixBranchName;
    private String destinationBranchName;

    public RestPrRepositoryPRTestAsserter(final String repositoryName, final String pullRequestLocation, final String pullRequestName, final String username,
            final String fixBranchName, final String destinationBranchName)
    {
        this.repositoryName = repositoryName;
        this.pullRequestLocation = pullRequestLocation;
        this.pullRequestName = pullRequestName;
        this.username = username;
        this.fixBranchName = fixBranchName;
        this.destinationBranchName = destinationBranchName;
    }

    public void assertBasicPullRequestConfiguration(final RestPrRepository restPrRepository)
    {
        boolean result = repositoryName.equals(restPrRepository.getSlug());

        Assert.assertEquals(restPrRepository.getPullRequests().size(), 1);

        RestPullRequest restPullRequest = restPrRepository.getPullRequests().get(0);
        Assert.assertTrue(pullRequestLocation.startsWith(restPullRequest.getUrl()));
        Assert.assertEquals(restPullRequest.getTitle(), pullRequestName);
        Assert.assertEquals(restPullRequest.getStatus(), PullRequestStatus.OPEN.toString());
        Assert.assertEquals(restPullRequest.getAuthor().getUsername(), username);
        Assert.assertEquals(restPullRequest.getSource().getBranch(), fixBranchName);
        final String expectedRepositorySlug = username + "/" + repositoryName;
        Assert.assertEquals(restPullRequest.getSource().getRepository(), expectedRepositorySlug);
        Assert.assertEquals(restPullRequest.getDestination().getBranch(), destinationBranchName);
        Assert.assertEquals(restPullRequest.getDestination().getRepository(), expectedRepositorySlug);
    }

    public void assertPullRequestApproved(final RestPullRequest restPullRequest)
    {

        Assert.assertEquals(restPullRequest.getParticipants().size(), 1);
        Assert.assertEquals(restPullRequest.getParticipants().get(0).getUser().getUsername(), username);
        Assert.assertTrue(restPullRequest.getParticipants().get(0).isApproved());
    }
}
