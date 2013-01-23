package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketBranchesAndTags;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

/**
 * Note: This Bitbucket restpoint is undocumented.
 *
 */
public class BranchesAndTagsRemoteRestpoint
{
    private final RemoteRequestor requestor;

    public BranchesAndTagsRemoteRestpoint(RemoteRequestor remoteRequestor)
    {
        this.requestor = remoteRequestor;
    }

    /**
     * <b>Requires authorization.</b>
     * 
     * @param owner
     * @param slug 
     * @return
     */
    public BitbucketBranchesAndTags getBranchesAndTags(String owner, String slug)
    {
        String getBranchesUrl = URLPathFormatter.format("/repositories/%s/%s/branches-tags", owner, slug);

        return requestor.get(getBranchesUrl, null, new ResponseCallback<BitbucketBranchesAndTags>()
        {
            @Override
            public BitbucketBranchesAndTags onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<BitbucketBranchesAndTags>()
                {
                }.getType());
            }
        });
    }
}
