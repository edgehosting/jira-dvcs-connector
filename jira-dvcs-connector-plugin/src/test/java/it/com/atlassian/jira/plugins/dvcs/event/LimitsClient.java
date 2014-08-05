package it.com.atlassian.jira.plugins.dvcs.event;

import com.atlassian.jira.webtests.util.JIRAEnvironmentData;
import com.beust.jcommander.internal.Maps;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.GenericType;
import com.sun.jersey.api.client.WebResource;
import it.com.atlassian.jira.plugins.dvcs.rest.DvcsRestClient;

import java.util.Map;
import javax.ws.rs.core.MediaType;

/**
 * REST client for the {@code bitbucket/1.0/event/limits} resource.
 */
public class LimitsClient extends DvcsRestClient<LimitsClient>
{
    public LimitsClient(JIRAEnvironmentData environmentData)
    {
        super(environmentData);
    }

    public LimitsResponse getLimits()
    {
        return toLimitsResponse(createResource().get(ClientResponse.class));
    }

    public LimitsResponse setLimits(Map<String, Integer> limits)
    {
        return toLimitsResponse(createResource().type(MediaType.APPLICATION_JSON).put(ClientResponse.class, asHashMap(limits)));
    }

    @Override
    protected WebResource createResource()
    {
        return super.createResource().path("event/limits");
    }

    private static Map<String, Integer> asHashMap(Map<String, Integer> map)
    {
        Map<String, Integer> hash = Maps.newHashMap();
        hash.putAll(map);

        return hash;
    }

    private LimitsResponse toLimitsResponse(final ClientResponse clientResponse)
    {
        return new LimitsResponse()
        {
            @Override
            public int status()
            {
                return clientResponse().getStatus();
            }

            @Override
            public Map<String, Integer> limits()
            {
                if (clientResponse().getStatus() != 200)
                {
                    throw new IllegalStateException("response status is " + clientResponse().getStatus());
                }

                return clientResponse.getEntity(new GenericType<Map<String, Integer>>() { });
            }

            @Override
            public ClientResponse clientResponse()
            {
                return clientResponse;
            }
        };
    }

}
