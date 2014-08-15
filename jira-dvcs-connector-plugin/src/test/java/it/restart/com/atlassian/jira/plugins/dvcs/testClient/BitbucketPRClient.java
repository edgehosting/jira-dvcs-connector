package it.restart.com.atlassian.jira.plugins.dvcs.testClient;

import com.atlassian.jira.plugins.dvcs.crypto.Encryptor;
import com.atlassian.jira.plugins.dvcs.model.Credential;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.BitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.DefaultBitbucketClientBuilderFactory;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.BitbucketRemoteClient;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.HttpClientProvider;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints.PullRequestRemoteRestpoint;

import java.util.Arrays;
import java.util.List;

public class BitbucketPRClient implements PullRequestClient
{
    @Override
    public PullRequestDetails openPullRequest(String owner, String repositoryName, String password, String title, String description, String head, String base, String... reviewers)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);
        List<String> reviewersList = reviewers == null ? null : Arrays.asList(reviewers);

        BitbucketPullRequest pullRequest = pullRequestRemoteRestpoint.createPullRequest(owner, repositoryName, title, description, head, base, reviewersList);

        return new PullRequestDetails(pullRequest.getLinks().getHtml().getHref(), pullRequest.getId());
    }

    @Override
    public PullRequestDetails updatePullRequest(String owner, String repositoryName, String password, BitbucketPullRequest pullRequest, String title, String description, String base)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        BitbucketPullRequest updatedPullRequest = pullRequestRemoteRestpoint.updatePullRequest(owner, repositoryName, pullRequest, title, description, base);

        return new PullRequestDetails(updatedPullRequest.getLinks().getHtml().getHref(), updatedPullRequest.getId());
    }

    @Override
    public PullRequestDetails openForkPullRequest(String owner, String repositoryName, String title, String description, String head, String base, String forkOwner, String forkPassword)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(forkOwner, forkPassword);

        BitbucketPullRequest pullRequest = pullRequestRemoteRestpoint.createPullRequest(owner, repositoryName, title, description, forkOwner, repositoryName, head, base);

        return new PullRequestDetails(pullRequest.getLinks().getHtml().getHref(), pullRequest.getId());
    }

    @Override
    public void declinePullRequest(String owner, String repositoryName, String password, Long pullRequestId)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        pullRequestRemoteRestpoint.declinePullRequest(owner, repositoryName, pullRequestId, null);
    }

    @Override
    public void approvePullRequest(String owner, String repositoryName, String password, Long pullRequestId)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        pullRequestRemoteRestpoint.approvePullRequest(owner, repositoryName, pullRequestId);
    }

    @Override
    public void mergePullRequest(String owner, String repositoryName, String password, Long pullRequestId)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);

        pullRequestRemoteRestpoint.mergePullRequest(owner, repositoryName, pullRequestId, "Merge message", true);
    }

    @Override
    public void commentPullRequest(String owner, String repositoryName, String password, Long pullRequestId, String comment)
    {
        PullRequestRemoteRestpoint pullRequestRemoteRestpoint = getPullRequestRemoteRestpoint(owner, password);
        pullRequestRemoteRestpoint.commentPullRequest(owner, repositoryName, pullRequestId, comment);
    }


    private PullRequestRemoteRestpoint getPullRequestRemoteRestpoint(String owner, String password)
    {
        BitbucketClientBuilderFactory bitbucketClientBuilderFactory = new DefaultBitbucketClientBuilderFactory(new Encryptor()
        {

            @Override
            public String encrypt(final String input, final String organizationName, final String hostUrl)
            {
                return input;
            }

            @Override
            public String decrypt(final String input, final String organizationName, final String hostUrl)
            {
                return input;
            }
        }, "DVCS Connector Tests", new HttpClientProvider());
        Credential credential = new Credential();
        credential.setAdminUsername(owner);
        credential.setAdminPassword(password);
        BitbucketRemoteClient bitbucketClient = bitbucketClientBuilderFactory.authClient("https://bitbucket.org", null, credential).apiVersion(2).build();
        return bitbucketClient.getPullRequestAndCommentsRemoteRestpoint();
    }
}
