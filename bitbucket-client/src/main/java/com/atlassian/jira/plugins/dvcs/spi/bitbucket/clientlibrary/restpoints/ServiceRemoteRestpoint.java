package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

public class ServiceRemoteRestpoint
{
    private RemoteRequestor requestor;

    public ServiceRemoteRestpoint(RemoteRequestor remoteRequestor)
    {
        this.requestor = remoteRequestor;
    }

    public BitbucketService addPOSTService(String owner, String slug, String serviceUrl)
    {
        String addServiceUrl = String.format("/repositories/%s/%s/services", owner, slug);

        Map<String, String> addServiceParameters = new HashMap<String, String>();
        addServiceParameters.put("type", "POST");
        addServiceParameters.put("URL", serviceUrl);

        return requestor.post(addServiceUrl, addServiceParameters, new ResponseCallback<BitbucketService>()
        {

            @Override
            public BitbucketService onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), BitbucketService.class);
            }

        });

    }

    public void deleteService(String owner, String slug, int serviceId)
    {
        String deleteServiceUrl = String.format("/repositories/%s/%s/services/%d", owner, slug, serviceId);

        requestor.delete(deleteServiceUrl, ResponseCallback.EMPTY);

    }

    public List<BitbucketServiceEnvelope> getAllServices(String owner, String slug)
    {
        String getAllServicesUrl = String.format("/repositories/%s/%s/services", owner, slug);

        return requestor.get(getAllServicesUrl, null, new ResponseCallback<List<BitbucketServiceEnvelope>>()
        {

            @Override
            public List<BitbucketServiceEnvelope> onResponse(RemoteResponse response)
            {
                return ClientUtils.fromJson(response.getResponse(), new TypeToken<List<BitbucketServiceEnvelope>>()
                {
                }.getType());
            }

        });

    }
}
