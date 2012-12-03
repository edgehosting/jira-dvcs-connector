package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.Set;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

/**
 * GroupRemoteRestpoint
 * 
 * 
 * <br />
 * <br />
 * Created on 13.7.2012, 17:29:24 <br />
 * <br />
 * 
 * @author jhocman@atlassian.com
 * 
 */
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
    public Set<BitbucketGroup> getGroups(String owner)
    {
        String getGroupUrl = String.format("/groups/%s", owner);

        return requestor.get(getGroupUrl, null, new ResponseCallback<Set<BitbucketGroup>>()
        {

            @Override
            public Set<BitbucketGroup> onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<Set<BitbucketGroup>>()
                {
                }.getType());
            }

        });

    }
}
