package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.HashMap;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;

public class ServicesRepositoryRemoteRestpoint
{
    private RemoteRequestor requestor;
    

	public ServicesRepositoryRemoteRestpoint(RemoteRequestor remoteRequestor)
	{
		this.requestor = remoteRequestor;
	}
    
    
    public BitbucketService addPOSTService(String owner, String slug, String serviceUrl)
    {
        String addServiceUrl = String.format("/repositories/%s/%s/services", owner, slug);
        
        Map<String, String> addServiceParameters = new HashMap<String, String>();
        addServiceParameters.put("type", "POST");
        addServiceParameters.put("URL", serviceUrl);
        
        RemoteResponse response = requestor.post(addServiceUrl, addServiceParameters);
        
        return ClientUtils.fromJson(response.getResponse(), BitbucketService.class);
    }
}

