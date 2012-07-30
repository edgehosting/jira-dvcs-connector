package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.List;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketSSHKey;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.google.gson.reflect.TypeToken;

/**
 * SSHRemoteRestpoint
 *
 * @author Martin Skurla mskurla@atlassian.com
 */
public class SSHRemoteRestpoint
{
    private final RemoteRequestor requestor;

    
    public SSHRemoteRestpoint(RemoteRequestor requestor)
    {
        this.requestor = requestor;
    }
    
    
    /**
     * <b>Requires authorization.</b>
     * 
     * @return 
     */
    public List<BitbucketSSHKey> getSSHKeys()
    {
        RemoteResponse response = requestor.get("/ssh-keys", null);
        
        return ClientUtils.fromJson(response.getResponse(), new TypeToken<List<BitbucketSSHKey>>(){}.getType());
    }
}
