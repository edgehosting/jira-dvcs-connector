package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.google.gson.reflect.TypeToken;

public class BitbucketPullRequestReviewerIterator extends BitbucketPageIterator<BitbucketPullRequestReviewer>
{
    /**
     * @param urlIncludingApi
     *
     *            /api/2.0/repositories/{user}/{slug}/pullrequests/{id}/reviewers
     */
    public BitbucketPullRequestReviewerIterator(RemoteRequestor requestor, String urlIncludingApi)
    {
        super(requestor, urlIncludingApi);
    }

    @Override
    protected BitbucketPullRequestPage<BitbucketPullRequestReviewer> transformFromJson(final RemoteResponse response)
    {
        return ClientUtils.fromJson(response.getResponse(),
                new TypeToken<BitbucketPullRequestPage<BitbucketPullRequestReviewer>>()
                {
                }.getType());
    }
}
