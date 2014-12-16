package com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint;

import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.pageobjects.RestUrlBuilder;
import com.google.common.base.Function;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;

import javax.ws.rs.core.MediaType;

/**
 * Endpoint for fetching entities by issuekey from the jira-dev endpoint based on the provided suffix
 *
 * @param <T> The type of the response, typically an inner class to assist with the JSON transform
 * @see com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.ChangesetLocalRestpoint
 * @see com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint.PullRequestLocalRestpoint
 */
public class EntityLocalRestpoint<T extends RestDevResponse>
{
    private final Class<T> clazz;
    private final String urlSuffix;

    /**
     * Create a local restpoint
     *
     * @param clazz The type of entity that will be returned from {@link #getEntity(String)}
     * @param urlSuffix The suffix between 'jira-dev/' and the issue key part of the request
     */
    public EntityLocalRestpoint(final Class<T> clazz, final String urlSuffix)
    {
        this.clazz = clazz;
        this.urlSuffix = urlSuffix;
    }

    /**
     * Fetch the T that matches the issueKey located at the jira-dev url suffix
     */
    public T getEntity(String issueKey)
    {
        RestUrlBuilder url = new RestUrlBuilder("/rest/bitbucket/1.0/jira-dev/" + urlSuffix + "?issue=" + issueKey);
        return fetchFromUrl(url);
    }

    /**
     * Try and fetch the entity for issue key, will retry every 100 millis for 100 times, a bit busy but this is code
     * running on the test server.
     */
    public T retryingGetEntity(String issueKey, Function<T, Boolean> predicate)
    {
        for (int i = 0; i < 100; i++)
        {
            T entity = getEntity(issueKey);
            if (predicate.apply(entity))
            {
                return entity;
            }
            else
            {
                try
                {
                    Thread.sleep(100);
                }
                catch (InterruptedException e)
                {
                    // just loop again
                }
            }
        }
        throw new UnsupportedOperationException("failed to retrieve entity that satisfies predicate before timeout");
    }

    private T fetchFromUrl(RestUrlBuilder url)
    {
        ClientConfig clientConfig = new DefaultClientConfig();
        clientConfig.getFeatures().put(JSONConfiguration.FEATURE_POJO_MAPPING, Boolean.TRUE);
        Client client = Client.create(clientConfig);
        return client.resource(url.toString()).accept(MediaType.APPLICATION_JSON_TYPE).get(clazz);
    }
}
