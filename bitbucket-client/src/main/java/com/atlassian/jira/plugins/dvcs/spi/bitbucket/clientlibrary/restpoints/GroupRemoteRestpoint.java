package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketGroup;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.google.gson.reflect.TypeToken;

/**
 * GroupRemoteRestpoint
 *
 * 
 * <br /><br />
 * Created on 13.7.2012, 17:29:24
 * <br /><br />
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
    public List<BitbucketGroup> getGroups(String owner)
    {
        String getGroupUrl = String.format("/groups/%s", owner);
        
        RemoteResponse response = requestor.get(getGroupUrl, null);
        
        return ClientUtils.fromJson(response.getResponse(),
                                    new TypeToken<List<BitbucketGroup>>(){}.getType());
    }
}

