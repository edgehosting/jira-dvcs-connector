package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.google.gson.reflect.TypeToken;

public class BitbucketPullRequestCommitIterator extends BitbucketPageIterator<BitbucketPullRequestCommit>
{
    /**
     * @param urlIncludingApi
     *            i.e.
     *            /api/2.0/repositories/erik/bitbucket/pullrequests/3/commits
     */
    public BitbucketPullRequestCommitIterator(RemoteRequestor requestor, String urlIncludingApi)
    {
        super(requestor, urlIncludingApi);
    }

    public BitbucketPullRequestCommitIterator(RemoteRequestor requestor, String urlIncludingApi, int requestLimit)
    {
        super(requestor, urlIncludingApi, requestLimit);
    }

    @Override
    protected BitbucketPullRequestPage<BitbucketPullRequestCommit> transformFromJson(final RemoteResponse response)
    {
        return ClientUtils.fromJson(response.getResponse(),
                new TypeToken<BitbucketPullRequestPage<BitbucketPullRequestCommit>>()
                {
                }.getType());
    }
}
