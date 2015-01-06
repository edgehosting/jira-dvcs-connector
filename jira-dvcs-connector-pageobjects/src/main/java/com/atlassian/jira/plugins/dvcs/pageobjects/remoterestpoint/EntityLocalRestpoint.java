package com.atlassian.jira.plugins.dvcs.pageobjects.remoterestpoint;

import com.atlassian.jira.pageobjects.JiraTestedProduct;
import com.atlassian.jira.plugins.dvcs.model.dev.RestDevResponse;
import com.atlassian.jira.plugins.dvcs.pageobjects.RestUrlBuilder;
import com.google.common.base.Function;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.api.json.JSONConfiguration;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
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
    private static final int NUMBER_OF_RETRIES = 100;
    private static final int DELAY_BETWEEN_RETRIES = 100;

    private final Class<T> entityClass;
    private final String urlSuffix;

    /**
     * Create a local restpoint
     *
     * @param entityClass The type of entity that will be returned from the getEntity methods
     * @param urlSuffix The suffix between 'jira-dev/' and the issue key part of the request
     */
    public EntityLocalRestpoint(@Nonnull final Class<T> entityClass, @Nonnull final String urlSuffix)
    {
        if (StringUtils.isBlank(urlSuffix))
        {
            throw new IllegalArgumentException("URL suffix for entity must not be blank");
        }
        this.entityClass = entityClass;
        this.urlSuffix = urlSuffix;
    }

    /**
     * Try and fetch the entity for issue key, will retry every 100 millis for 100 times, a bit busy but this is code
     * running on the test server.
     *
     * @param issueKey The issue key to search for
     * @param predicate The predicate to decide if the right information of the issue key is returned
     * @return The entity of type T
     */
    public T getEntity(String issueKey, Function<T, Boolean> predicate)
    {
        return getEntity(issueKey, predicate, null);
    }

    /**
     * Try and fetch the entity for issue key, will retry every 100 millis for 100 times, a bit busy but this is code
     * running on the test server.
     *
     * @param issueKey The issue key to search for
     * @param predicate The predicate to decide if the right information of the issue key is returned
     * @param jira The @{link JiraTestedProduct} to use to build the URL
     * @return The entity of type T
     */
    public T getEntity(String issueKey, Function<T, Boolean> predicate, JiraTestedProduct jira)
    {
        String urlText = String.format("/rest/bitbucket/1.0/jira-dev/%s?issue=%s", urlSuffix, issueKey);
        RestUrlBuilder url = jira == null ? new RestUrlBuilder(urlText) : new RestUrlBuilder(jira, urlText);

        for (int i = 0; i < NUMBER_OF_RETRIES; i++)
        {
            T entity = fetchFromUrl(url);
            if (predicate.apply(entity))
            {
                return entity;
            }
            else
            {
                try
                {
                    Thread.sleep(DELAY_BETWEEN_RETRIES);
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
        return client.resource(url.toString()).accept(MediaType.APPLICATION_JSON_TYPE).get(entityClass);
    }
}
