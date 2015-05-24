package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

import java.util.List;

public class GroupRemoteRestpoint
{
    private RemoteRequestor requestor;

    public GroupRemoteRestpoint(RemoteRequestor remoteRequestor)
    {
        this.requestor = remoteRequestor;
    }

    /**
     * <b>Requires authorization.</b>
     * 
     * @param owner
     * @return
     */
    public List<BitbucketGroup> getGroups(String owner)
    {
        String getGroupUrl = URLPathFormatter.format("/groups/%s", owner);

        return requestor.get(getGroupUrl, null, new ResponseCallback<List<BitbucketGroup>>()
        {

            @Override
            public List<BitbucketGroup> onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<List<BitbucketGroup>>()
                {
                }.getType());
            }
        });
    }
}
