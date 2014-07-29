package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketUser;
import org.apache.http.entity.ContentType;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranch;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequest;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestActivityInfo;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestApprovalActivity;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestApprovalsIterator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommit;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestCommitIterator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestHead;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestPage;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestRepository;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestReviewer;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketPullRequestReviewerIterator;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

/**
 *
 * PullRequestRemoteRestpoint
 *
 *
 * <br /><br />
 * Created on 11.12.2012, 13:14:31
 * <br /><br />
 * @author jhocman@atlassian.com
 *
 */
public class PullRequestRemoteRestpoint
{

    public static final int REPO_ACTIVITY_PAGESIZE = 30;

    private final RemoteRequestor requestor;

    public PullRequestRemoteRestpoint(RemoteRequestor requestor)
    {
        this.requestor = requestor;
    }

    public BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> getRepositoryActivityPage(int page, String owner, String repoSlug, final Date upToDate) {

        String activityUrl = String.format("/repositories/%s/%s/pullrequests/activity?pagelen=%s&page=%s", owner, repoSlug, REPO_ACTIVITY_PAGESIZE, page);
        ResponseCallback<BitbucketPullRequestPage<BitbucketPullRequestActivityInfo>> callback = new ResponseCallback<BitbucketPullRequestPage<BitbucketPullRequestActivityInfo>>()
        {
            @Override
            public BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> onResponse(RemoteResponse response)
            {
                BitbucketPullRequestPage<BitbucketPullRequestActivityInfo> remote =
                        ClientUtils.fromJson(response.getResponse(),new TypeToken<BitbucketPullRequestPage<BitbucketPullRequestActivityInfo>>(){}.getType() );

                if (remote != null && remote.getValues() != null && !remote.getValues().isEmpty())
                {
                    // filter by date here
                    remote.setValues(filterByDate(upToDate, remote.getValues()));
                }

                return remote;
            }

            private List<BitbucketPullRequestActivityInfo> filterByDate(Date upToDate, List<BitbucketPullRequestActivityInfo> values)
            {
                List<BitbucketPullRequestActivityInfo> fine = new ArrayList<BitbucketPullRequestActivityInfo>();
                for (BitbucketPullRequestActivityInfo info : values)
                {
                    Date activityDate = ClientUtils.extractActivityDate(info.getActivity());
                    if (upToDate == null || activityDate != null && activityDate.after(upToDate))
                    {
                        fine.add(info);
                    }
                }
                return fine;
            }
        };
        return requestor.get(activityUrl, null, callback);

    }

    public Iterable<BitbucketPullRequestCommit> getPullRequestCommits(String owner, String repoSlug, String localId, int requestLimit)
    {
        String url = String.format("/repositories/%s/%s/pullrequests/%s/commits", owner, repoSlug, localId);

        return new BitbucketPullRequestCommitIterator(requestor, url, requestLimit);
    }

    public Iterable<BitbucketPullRequestCommit> getPullRequestCommits(String urlIncludingApi, int requestLimit)
    {
        return new BitbucketPullRequestCommitIterator(requestor, urlIncludingApi, requestLimit);
    }

    public BitbucketPullRequest getPullRequestDetail(String owner, String repoSlug, String localId)
    {

        String url = String.format("/repositories/%s/%s/pullrequests/%s", owner, repoSlug, localId);

        return getPullRequestDetail(url);
    }

    public BitbucketPullRequest getPullRequestDetail(String urlIncludingApi)
    {
        return requestor.get(urlIncludingApi, null, new ResponseCallback<BitbucketPullRequest>()
        {

            @Override
            public BitbucketPullRequest onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketPullRequest>()
                {
                }.getType());
            }

        });
    }

    public Iterable<BitbucketPullRequestReviewer> getPullRequestReviewers(String urlIncludingApi)
    {
        return new BitbucketPullRequestReviewerIterator(requestor, urlIncludingApi);
    }

    public Iterable<BitbucketPullRequestApprovalActivity> getPullRequestApprovals(String urlIncludingApi)
    {
        return new BitbucketPullRequestApprovalsIterator(requestor, urlIncludingApi);
    }

    public int getCount(String urlIncludingApi)
    {
        String url = urlIncludingApi + "?pagelen=0";

        return requestor.get(url, null, new ResponseCallback<Integer>()
        {
            @Override
            public Integer onResponse(RemoteResponse response)
            {

                BitbucketPullRequestPage<?> remote = transformFromJson(response);
                if (remote.getSize() == null)
                {
                    return 0;
                }
                return remote.getSize();
            }
        });
    }

    private BitbucketPullRequestPage<?> transformFromJson(final RemoteResponse response)
    {
        return ClientUtils.fromJson(response.getResponse(),
                new TypeToken<BitbucketPullRequestPage<?>>()
                {
                }.getType());
    }

    public BitbucketPullRequest createPullRequest(String owner, String repoSlug, String title, String description, String sourceBranch, String destinationBranch, List<String> reviewers)
    {
        return createBitbucketPullRequest(owner, repoSlug, title, description, null, sourceBranch, destinationBranch, reviewers);
    }

    public BitbucketPullRequest createPullRequest(String owner, String repoSlug, String title, String description, String sourceOwner, String sourceRepository, String sourceBranch, String destinationBranch)
    {
        BitbucketPullRequestRepository bitbucketPullRequestRepository = new BitbucketPullRequestRepository();
        bitbucketPullRequestRepository.setFullName(sourceOwner + "/" + sourceRepository);
        return createBitbucketPullRequest(owner, repoSlug, title, description, bitbucketPullRequestRepository, sourceBranch, destinationBranch, null);
    }

    public BitbucketPullRequest updatePullRequest(String owner, String repoSlug, BitbucketPullRequest pullRequest, String title, String description, String destinationBranch)
    {
        return updateBitbucketPullRequest(owner, repoSlug, pullRequest, title, description, destinationBranch);
    }

    public void declinePullRequest(String owner, String repoSlug, long pullRequestId, String message)
    {
        String url = String.format("/repositories/%s/%s/pullrequests/%s/decline", owner, repoSlug, pullRequestId);

        Map<String, String> parameters = null;
        if (message != null)
        {
            parameters = new HashMap<String, String>();
            parameters.put("message", message);
        }

        requestor.post(url, parameters, ResponseCallback.EMPTY);
    }

    public void approvePullRequest(final String owner, final String repoSlug, final long pullRequestId)
    {
        String url = String.format("/repositories/%s/%s/pullrequests/%s/approve", owner, repoSlug, pullRequestId);

        requestor.post(url, null, ResponseCallback.EMPTY);
    }

    public void mergePullRequest(String owner, String repoSlug, long pullRequestId, String message, boolean closeSourceBranch)
    {
        String url = String.format("/repositories/%s/%s/pullrequests/%s/merge", owner, repoSlug, pullRequestId);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("message", message);
        parameters.put("close_source_branch", Boolean.toString(closeSourceBranch));

        requestor.post(url, parameters, ResponseCallback.EMPTY);
    }

    public void commentPullRequest(String owner, String repoSlug, long pullRequestId, String comment)
    {
        String url = String.format("/api/1.0/repositories/%s/%s/pullrequests/%s/comments", owner, repoSlug, pullRequestId);

        Map<String, String> parameters = new HashMap<String, String>();
        parameters.put("content", comment);

        requestor.post(url, parameters, ResponseCallback.EMPTY);
    }

    private BitbucketPullRequest createBitbucketPullRequest(final String owner, final String repoSlug, final String title, final String description, final BitbucketPullRequestRepository sourceRepository, final String sourceBranch, final String destinationBranch, final List<String> reviewers)
    {
        BitbucketPullRequest bitbucketPullRequest = new BitbucketPullRequest();
        bitbucketPullRequest.setTitle(title);
        bitbucketPullRequest.setDescription(description);

        BitbucketPullRequestHead source = new BitbucketPullRequestHead();
        source.setBranch(new BitbucketBranch(sourceBranch));
        source.setRepository(sourceRepository);
        bitbucketPullRequest.setSource(source);

        BitbucketPullRequestHead destination = new BitbucketPullRequestHead();
        destination.setBranch(new BitbucketBranch(destinationBranch));
        bitbucketPullRequest.setDestination(destination);

        if (reviewers != null)
        {
            List<BitbucketUser> bitbucketReviewers = new ArrayList<BitbucketUser>();
            for (String reviewer : reviewers)
            {
                BitbucketUser bitbucketReviewer = new BitbucketUser();
                bitbucketReviewer.setUsername(reviewer);
                bitbucketReviewers.add(bitbucketReviewer);
            }
            bitbucketPullRequest.setReviewers(bitbucketReviewers);
        }

        String url = String.format("/repositories/%s/%s/pullrequests", owner, repoSlug);

        return requestor.post(url, ClientUtils.toJson(bitbucketPullRequest).toString(), ContentType.APPLICATION_JSON, new ResponseCallback<BitbucketPullRequest>()
        {

            @Override
            public BitbucketPullRequest onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketPullRequest>()
                {
                }.getType());
            }

        });
    }

    private BitbucketPullRequest updateBitbucketPullRequest(String owner, String repoSlug, BitbucketPullRequest pullRequest, String title, String description, String destinationBranch)
    {
        pullRequest.setTitle(title);
        pullRequest.setDescription(description);

        pullRequest.setSource(null);

        BitbucketPullRequestHead destination = new BitbucketPullRequestHead();
        destination.setBranch(new BitbucketBranch(destinationBranch));
        pullRequest.setDestination(destination);

        String url = String.format("/repositories/%s/%s/pullrequests/%s", owner, repoSlug, pullRequest.getId());

        return requestor.put(url, ClientUtils.toJson(pullRequest).toString(), ContentType.APPLICATION_JSON, new ResponseCallback<BitbucketPullRequest>()
        {

            @Override
            public BitbucketPullRequest onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketPullRequest>()
                {
                }.getType());
            }

        });
    }
}

