package com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.restpoints;

import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.client.ClientUtils;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketService;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.model.BitbucketServiceEnvelope;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteRequestor;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.RemoteResponse;
import com.atlassian.jira.plugins.dvcs.spi.bitbucket.clientlibrary.request.ResponseCallback;
import com.google.gson.reflect.TypeToken;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServiceRemoteRestpoint
{
    public static final String SERVICE_TYPE_POST = "POST";
    public static final String SERVICE_TYPE_PULL_REQUEST_POST = "Pull Request POST";

    private final RemoteRequestor requestor;

    public ServiceRemoteRestpoint(RemoteRequestor remoteRequestor)
    {
        this.requestor = remoteRequestor;
    }

    public BitbucketService addPOSTService(String owner, String slug, String serviceUrl)
    {
        return addService(owner, slug, serviceUrl, SERVICE_TYPE_POST);
    }

    public BitbucketService addPullRequestPOSTService(String owner, String slug, String serviceUrl)
    {
        return addService(owner, slug, serviceUrl, SERVICE_TYPE_PULL_REQUEST_POST);
    }

    private BitbucketService addService(final String owner, final String slug, final String serviceUrl, final String serviceType)
    {
        String addServiceUrl = URLPathFormatter.format("/repositories/%s/%s/services", owner, slug);

        Map<String, String> addServiceParameters = new HashMap<String, String>();
        addServiceParameters.put("type", serviceType);
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
        String deleteServiceUrl = URLPathFormatter.format("/repositories/%s/%s/services/%s", owner, slug, "" + serviceId);

        requestor.delete(deleteServiceUrl, Collections.<String, String>emptyMap(), ResponseCallback.EMPTY);
    }

    public List<BitbucketServiceEnvelope> getAllServices(String owner, String slug)
    {
        String getAllServicesUrl = URLPathFormatter.format("/repositories/%s/%s/services", owner, slug);

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
