package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketAccount;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;

/**
 * AccountRemoteRestpoint
 * 
 * @author Martin Skurla mskurla@atlassian.com
 */
public class AccountRemoteRestpoint {
    private final RemoteRequestor requestor;

    
    public AccountRemoteRestpoint(RemoteRequestor remoteRequestor)
    {
        this.requestor = remoteRequestor;
    }

    
    public BitbucketAccount getUser(String user)
    {
        String getUserUrl = String.format("/users/%s", user);
        
        RemoteResponse response = requestor.get(getUserUrl, null);
        
        return ClientUtils.fromJson(response.getResponse(), BitbucketAccount.class);
    }
}
