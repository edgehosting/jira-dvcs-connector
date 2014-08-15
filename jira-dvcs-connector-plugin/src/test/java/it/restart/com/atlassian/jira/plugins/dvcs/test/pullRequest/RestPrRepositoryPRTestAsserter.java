package it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest;

import com.atlassian.jira.plugins.dvcs.model.PullRequestStatus;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrCommit;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPrRepository;
import com.atlassian.jira.plugins.dvcs.model.dev.RestPullRequest;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.testng.Assert;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Utility class that wraps up some of the common assertions in {@link it.restart.com.atlassian.jira.plugins.dvcs.test.pullRequest.PullRequestTestCases}
 */
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

    public void assertBasicPullRequestConfiguration(final RestPrRepository restPrRepository, final Collection<String> commits)
    {
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

        assertCommitsMatch(restPullRequest, commits);
    }

    public void assertCommitsMatch(final RestPullRequest restPullRequest, final Collection<String> commits)
    {
        List<RestPrCommit> restCommits = restPullRequest.getCommits();

        List<String> actualCommits = Lists.transform(restCommits, new Function<RestPrCommit, String>()
        {
            @Override
            public String apply(@Nullable final RestPrCommit restPrCommit)
            {
                return restPrCommit.getNode();
            }
        });

        MatcherAssert.assertThat(actualCommits, Matchers.containsInAnyOrder(commits.toArray()));
    }

    public void assertPullRequestApproved(final RestPullRequest restPullRequest)
    {

        Assert.assertEquals(restPullRequest.getParticipants().size(), 1);
        Assert.assertEquals(restPullRequest.getParticipants().get(0).getUser().getUsername(), username);
        Assert.assertTrue(restPullRequest.getParticipants().get(0).isApproved());
    }
}
