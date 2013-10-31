package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.google.gson.reflect.TypeToken;

public class BitbucketPullRequestApprovalsIterator extends BitbucketPageIterator<BitbucketPullRequestApprovalActivity>
{
    /**
     * @param urlIncludingApi
     *
     *            /api/2.0/repositories/{user}/{slug}/pullrequests/{id}/approvals
     */
    public BitbucketPullRequestApprovalsIterator(RemoteRequestor requestor, String urlIncludingApi)
    {
        super(requestor, urlIncludingApi);
    }

    @Override
    protected BitbucketPullRequestPage<BitbucketPullRequestApprovalActivity> transformFromJson(final RemoteResponse response)
    {
        return ClientUtils.fromJson(response.getResponse(),
                new TypeToken<BitbucketPullRequestPage<BitbucketPullRequestApprovalActivity>>()
                {
                }.getType());
    }
}
